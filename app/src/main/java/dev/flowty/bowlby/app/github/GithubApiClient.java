package dev.flowty.bowlby.app.github;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.flowty.bowlby.app.github.Entity.Artifact;
import dev.flowty.bowlby.app.github.Entity.Branch;
import dev.flowty.bowlby.app.github.Entity.NamedArtifact;
import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.github.Entity.Run;
import dev.flowty.bowlby.app.github.Entity.Workflow;
import dev.flowty.bowlby.app.github.Message.GetRepoResponse;
import dev.flowty.bowlby.app.github.Message.ListWorkflowRunArtifactsResponse;
import dev.flowty.bowlby.app.github.Message.ListWorkflowRunResponse;

/**
 * Supports our interactions with the github API.
 */
public class GithubApiClient {
  private static final String API_VERSION = "2022-11-28";

  /**
   * The number of seconds of inter-call interval above which we suspect something
   * is going wrong with our API usage. We'll start logging warnings and
   * aggressively ramping up the interval.
   */
  private static final int CALL_INTERVAL_BACKOFF_LIMIT = 2;

  private static final Logger LOG = LoggerFactory.getLogger( GithubApiClient.class );

  private final String apiHost;
  private final String authToken;
  private final HttpClient http = HttpClient.newBuilder()
      .version( Version.HTTP_1_1 )
      .build();

  /**
   * Holds the last time that we made an api call.
   */
  private Instant lastCall = Instant.now();
  /**
   * Holds the current minimum duration required between api calls in order that
   * we don't break the rate limits. See <a href=
   * "https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api">
   * the API limit docs</a>.
   */
  private Duration callInterval = Duration.ZERO;

  /**
   * @param apiHost   The hostname to hit with our requests
   * @param authToken The token to put on our requests
   */
  public GithubApiClient( String apiHost, String authToken ) {
    this.apiHost = apiHost;
    this.authToken = authToken;
  }

  /**
   * Downloads an artifact. This is a two-stage process:
   * <ol>
   * <li>Hit the API to get a download link</li>
   * <li>Use the download link</li>
   * </ol>
   *
   * @param artifact    The ID of the artifact
   * @param destination The file to download to
   * @return The downloaded file path, or <code>null</code> on failure
   */
  public Path getArtifact( Artifact artifact, Path destination ) {
    LOG.info( "Downloading artifact {} to ", artifact, destination );
    try {
      HttpResponse<String> redirect = send(
          HttpRequest.newBuilder()
              .GET()
              .uri( new URI( String.format(
                  "%s/repos/%s/%s/actions/artifacts/%s/zip",
                  apiHost, artifact.repo().owner(), artifact.repo().repo(), artifact.id() ) ) )
              .header( "Accept", "application/vnd.github+json" )
              .header( "Authorization", "Bearer " + authToken )
              .header( "X-GitHub-Api-Version", API_VERSION )
              .build(),
          BodyHandlers.ofString() );

      Optional<String> dlUri = redirect.headers().firstValue( "location" );
      if( redirect.statusCode() != 302 && dlUri.isEmpty() ) {
        LOG.error( "Failed to get download URL {}/{}",
            redirect.statusCode(), redirect.body() );
      }
      else {
        LOG.debug( "Downloading from {}", dlUri.get() );
        Files.createDirectories( destination.getParent() );

        // The download link does not count towards API limits
        HttpResponse<Path> dl = http.send(
            HttpRequest.newBuilder()
                .GET()
                .uri( new URI( dlUri.get() ) )
                .build(),
            BodyHandlers.ofFile( destination, CREATE, TRUNCATE_EXISTING, WRITE ) );

        LOG.info( "Downloaded to {}", dl.body() );

        return dl.body();
      }
    }
    catch( IOException | InterruptedException | URISyntaxException e ) {
      LOG.error( "Failed to download artifact {}", artifact, e );
    }

    return null;
  }

  /**
   * Gets the latest runs of a workflow
   *
   * @param workflow The workflow
   * @param branch   The branch on which the workflow was run
   * @return The latest run ID of that workflow on that branch, or
   *         <code>null</code> on failure
   */
  public Run getLatestRun( Workflow workflow, Branch branch ) {
    LOG.info( "Getting latest run of {}", workflow );
    try {
      HttpResponse<ListWorkflowRunResponse> response = send( HttpRequest.newBuilder()
          .GET()
          .uri( new URI( String.format(
              "%s/repos/%s/%s/actions/workflows/%s/runs?branch=%s&status=completed&per_page=1",
              apiHost,
              workflow.repo().owner(), workflow.repo().repo(), workflow.name(), branch.name() ) ) )
          .header( "Accept", "application/vnd.github+json" )
          .header( "Authorization", "Bearer " + authToken )
          .header( "X-GitHub-Api-Version", API_VERSION )
          .build(),
          ListWorkflowRunResponse.HANDLER );

      if( response.statusCode() != 200 ) {
        LOG.error( "Unexpected response status {}. Run with {} to see the full response.",
            response.statusCode(),
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=trace" );
        return null;
      }

      return Optional.ofNullable( response )
          .map( HttpResponse::body )
          .map( b -> b.runs )
          .filter( l -> !l.isEmpty() )
          .map( l -> l.get( 0 ) )
          .map( msg -> new Run( workflow, msg.id ) )
          .orElse( null );
    }
    catch( IOException | InterruptedException | URISyntaxException e ) {
      LOG.error( "Failed to find latest run of {}", workflow, e );
    }
    return null;
  }

  /**
   * Gets the names and IDs of the artifacts on a workflow run
   *
   * @param run The run ID of that workflow
   * @return The set of artifacts from the run, or <code>null</code> on failure
   */
  public Set<NamedArtifact> getArtifacts( Run run ) {
    LOG.info( "Getting artifacts of {}", run );
    try {
      HttpResponse<ListWorkflowRunArtifactsResponse> response = send( HttpRequest.newBuilder()
          .GET()
          .uri( new URI( String.format(
              "%s/repos/%s/%s/actions/runs/%s/artifacts",
              apiHost,
              run.flow().repo().owner(), run.flow().repo().repo(), run.id() ) ) )
          .header( "Accept", "application/vnd.github+json" )
          .header( "Authorization", "Bearer " + authToken )
          .header( "X-GitHub-Api-Version", API_VERSION )
          .build(),
          ListWorkflowRunArtifactsResponse.HANDLER );

      if( response.statusCode() != 200 ) {
        LOG.error( "Unexpected response status {}. Run with {} to see the full response.",
            response.statusCode(),
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=trace" );
        return null;
      }

      return Optional.ofNullable( response )
          .map( HttpResponse::body )
          .map( body -> body.artifacts )
          .map( list -> list.stream()
              .map( msg -> new NamedArtifact(
                  new Artifact( run.flow().repo(), msg.id ),
                  msg.name ) )
              .collect( toCollection( () -> new TreeSet<>( NamedArtifact.ORDER ) ) ) )
          .orElse( new TreeSet<>() );
    }
    catch( IOException | InterruptedException | URISyntaxException e ) {
      LOG.error( "Failed to find artifacts of {}", run, e );
    }
    return null;
  }

  /**
   * Gets the default branch of a repository
   *
   * @param repo The repository
   * @return The default branch name
   */
  public Branch getDefaultBranch( Repository repo ) {
    LOG.info( "Getting details of {}", repo );
    try {
      HttpResponse<GetRepoResponse> response = send( HttpRequest.newBuilder()
          .GET()
          .uri( new URI( String.format(
              "%s/repos/%s/%s",
              apiHost,
              repo.owner(), repo.repo() ) ) )
          .header( "Accept", "application/vnd.github+json" )
          .header( "Authorization", "Bearer " + authToken )
          .header( "X-GitHub-Api-Version", API_VERSION )
          .build(),
          GetRepoResponse.HANDLER );

      if( response.statusCode() != 200 ) {
        LOG.error( "Unexpected response status {}. Run with {} to see the full response.",
            response.statusCode(),
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=trace" );
        return null;
      }

      return new Branch( repo, response.body().defaultBranch );
    }
    catch( IOException | InterruptedException | URISyntaxException e ) {
      LOG.error( "Failed to get details of {}", repo, e );
    }
    return null;
  }

  /**
   * Sends a API request, while trying to keep within the rate limits. The
   * rate-limiting behaviour is the reason for the synchronization - it
   * <i>might</i> work in a multithreaded context, but it's difficult to tell. I
   * expect our API usage to be low enough that the synchronization will never be
   * a problem.
   *
   * @param <T>     Response body type
   * @param request API request
   * @param handler How to turn the response body into the desired type
   * @return API response
   * @throws IOException          on failure
   * @throws InterruptedException seems unlikely
   */
  private synchronized <T> HttpResponse<T> send( HttpRequest request, BodyHandler<T> handler )
      throws IOException, InterruptedException {

    Instant next = lastCall.plus( callInterval );
    Instant now = Instant.now();
    while( now.isBefore( next ) ) {
      try {
        Thread.sleep( Duration.between( now, next ).toMillis() );
      }
      catch( InterruptedException e ) {
        LOG.warn( "unexpected interruption", e );
      }
      now = Instant.now();
    }

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Sending request\n{} {}\n{}",
          request.method(), request.uri(),
          request.headers().map().entrySet().stream()
              .map( e -> e.getKey() + ": " + e.getValue().stream()
                  .map( v -> "authorization".equalsIgnoreCase( e.getKey() )
                      ? "_masked_secret_"
                      : v )
                  .collect( joining( ", " ) ) )
              .collect( joining( "\n" ) ) );
    }
    HttpResponse<T> response = http.send( request, handler );
    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Got response\n{} {}\n{}\n{}",
          response.version(), response.statusCode(),
          response.headers().map().entrySet().stream()
              .map( e -> e.getKey() + ": " + e.getValue().stream().collect( joining( ", " ) ) )
              .collect( joining( "\n" ) ),
          Message.toJson( response.body() ) );
    }

    lastCall = Instant.now();
    updateInterval(
        response.headers().firstValue( "x-ratelimit-remaining" ),
        response.headers().firstValue( "x-ratelimit-reset" ) );

    return response;
  }

  private void updateInterval( Optional<String> remainingCount, Optional<String> resetUTC ) {
    try {
      if( remainingCount.isEmpty() || resetUTC.isEmpty() ) {
        LOG.warn( "Missing rate-limit headers!" );
        return;
      }
      int remaining = Integer.parseInt( remainingCount.get() );
      Instant reset = Instant.ofEpochSecond( Long.parseLong( resetUTC.get() ) );
      Duration toReset = Duration.between( lastCall, reset );
      callInterval = toReset.dividedBy( remaining );
      LOG.debug( "New API call interval {}, thanks to {} calls remaining for the next {}",
          callInterval, remaining, toReset );

      if( callInterval.getSeconds() > CALL_INTERVAL_BACKOFF_LIMIT ) {
        // we really don't expect heavy usage of the API, so if the rate limit ever goes
        // above a trivial value then something weird is afoot
        // exponential backoff should keep us on the level
        callInterval = callInterval.multipliedBy( callInterval.getSeconds() );
        LOG.warn( """
            Suspicious rate-limit behaviour! {} calls remaining for the next {}.\
             Interval increased to {}""",
            remaining, toReset, callInterval );
      }
    }
    catch( Exception e ) {
      LOG.error( "Failed to update rate limiting from remaining {} and reset {}",
          remainingCount, resetUTC, e );
    }
  }
}
