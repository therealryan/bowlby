package dev.flowty.bowlby.app;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the cache of downloaded artifacts
 */
public class Artifacts {

  private static final Logger LOG = LoggerFactory.getLogger( Artifacts.class );

  private static final Path DOWNLOAD_ROOT = Paths.get(
      System.getProperty( "java.io.tmpdir" ),
      "bowlby" );

  private final String githubApiHost;
  private final String authToken;

  /**
   * @param githubApiHost The host name to send API requests to
   * @param authToken     The authorisation token to use for API requests
   */
  public Artifacts( String githubApiHost, String authToken ) {
    this.githubApiHost = githubApiHost;
    this.authToken = authToken;
  }

  private static final HttpClient HTTP = HttpClient.newBuilder().build();

  /**
   * Gets an artifact file path, downloading it if necessary
   *
   * @param repository The repository
   * @param artifactId The ID of the artifact
   * @return The path to the artifact zip file, or <code>null</code> if it could
   *         not be retrieved
   */
  public Path get( GitHubRepository repository, String artifactId ) {

    Path destination = DOWNLOAD_ROOT
        .resolve( repository.owner() )
        .resolve( repository.repo() )
        .resolve( artifactId + ".zip" );

    if( Files.exists( destination ) ) {
      return destination;
    }

    LOG.info( "Downloading artifact {}/{}", repository, artifactId );

    // downloading artifacts is a two-step process:

    // First we hit the API to get the download URL
    try {
      HttpResponse<String> redirect = HTTP.send(
          HttpRequest.newBuilder()
              .GET()
              .uri( new URI( String.format(
                  "%s/repos/%s/%s/actions/artifacts/%s/zip",
                  githubApiHost, repository.owner(), repository.repo(), artifactId ) ) )
              .header( "Accept", "application/vnd.github+json" )
              .header( "Authorization", "Bearer " + authToken )
              .header( "X-GitHub-Api-Version", "2022-11-28" )
              .build(),
          BodyHandlers.ofString() );

      Optional<String> dlUri = redirect.headers().firstValue( "location" );
      if( redirect.statusCode() != 302 && dlUri.isEmpty() ) {
        LOG.warn( "Failed to get download URL {}/{}",
            redirect.statusCode(), redirect.body() );
      }
      else {
        LOG.debug( "Downloading from {}", dlUri.get() );
        Files.createDirectories( destination.getParent() );
        // then we hit that download URL
        HttpResponse<Path> dl = HTTP.send(
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
      LOG.error( "Failed to download artifact {}/{}", repository, artifactId, e );
    }

    return null;
  }

  /**
   * Lists the repo owners for which we have cached artifacts
   *
   * @return The set of owners
   */
  public static Set<String> listOwners() {
    Path dir = DOWNLOAD_ROOT;
    if( !Files.exists( dir ) || !Files.isDirectory( dir ) ) {
      return Collections.emptySet();
    }

    try( Stream<Path> files = Files.list( dir ) ) {
      return files.filter( Files::isDirectory )
          .map( path -> path.getFileName().toString() )
          .collect( toCollection( TreeSet::new ) );
    }
    catch( IOException e ) {
      LOG.warn( "Failed to list owners", e );
      return Collections.emptySet();
    }
  }

  /**
   * Lists the repos for which we have cached artifacts
   *
   * @param owner The owner of the repos
   * @return The set of ownder repos
   */
  public static Set<String> listRepos( String owner ) {
    Path dir = DOWNLOAD_ROOT
        .resolve( owner );
    if( !Files.exists( dir ) || !Files.isDirectory( dir ) ) {
      return Collections.emptySet();
    }
    try( Stream<Path> files = Files.list( dir ) ) {
      return files.filter( Files::isDirectory )
          .map( path -> path.getFileName().toString() )
          .collect( toCollection( TreeSet::new ) );
    }
    catch( IOException e ) {
      LOG.warn( "Failed to list repos for {}", owner, e );
      return Collections.emptySet();
    }
  }

  /**
   * Lists the artifact IDs that we have cached
   *
   * @param repo The repo that owns the artifacts
   * @return The artifact IDs
   */
  public static Set<String> listArtifacts( GitHubRepository repo ) {
    Path dir = DOWNLOAD_ROOT
        .resolve( repo.owner() )
        .resolve( repo.repo() );
    if( !Files.exists( dir ) || !Files.isDirectory( dir ) ) {
      return Collections.emptySet();
    }
    try( Stream<Path> files = Files.list( dir ) ) {
      return files.filter( Files::isRegularFile )
          .map( path -> path.getFileName().toString() )
          .filter( file -> file.endsWith( ".zip" ) )
          .map( file -> file.substring( 0, file.length() - 4 ) )
          .collect( toCollection( TreeSet::new ) );
    }
    catch( IOException e ) {
      LOG.warn( "Failed to list zips for {}", repo, e );
      return Collections.emptySet();
    }
  }
}
