package dev.flowty.bowlby.app.srv;

import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.github.GithubApiClient;

/**
 * Handles requests to <code>/latest/owner/repo/workflow/artifact</code>,
 * redirects to <code>/artifacts/owner/repo/artifactId</code>.
 */
public class LatestArtifactHandler implements HttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger( LatestArtifactHandler.class );

  /**
   * The set of repos that we allow ourselves to serve from
   */
  private final Set<Repository> repos;
  private final GithubApiClient client;

  /**
   * @param repos  The set of repos that we allow ourselves to serve from
   * @param client How to interact with github
   */
  public LatestArtifactHandler( Set<Repository> repos, GithubApiClient client ) {
    this.repos = repos;
    this.client = client;
  }

  @Override
  public void handle( HttpExchange exchange ) throws IOException {
    try {
      LOG.debug( "{}", exchange.getRequestURI() );
      if( !"GET".equalsIgnoreCase( exchange.getRequestMethod() ) ) {
        ServeUtil.showLinkForm( exchange, 501, "Only GET supported" );
        return;
      }

      Deque<String> path = Stream.of( exchange.getRequestURI().getPath().split( "/" ) )
          .filter( e -> !e.isEmpty() )
          .collect( toCollection( ArrayDeque::new ) );
      if( path.size() < 5 ) {
        ServeUtil.showLinkForm( exchange, 404, "insufficient path" );
        return;
      }
      if( !"latest".equals( path.poll() ) ) {
        ServeUtil.showLinkForm( exchange, 500, "unexpected root" );
      }
      Repository repo = new Repository( path.poll(), path.poll() );
      if( !repos.isEmpty() && !repos.contains( repo ) ) {
        // we're limited to particular repos, and that isn't one of them
        ServeUtil.showLinkForm( exchange, 403, "forbidden repository addressed" );
        return;
      }

      ServeUtil.respond( exchange, 0, "doing the api and redirecting to artifact for " + path );
    }
    catch( Exception e ) {
      LOG.error( "request handling failure!", e );
      ServeUtil.showLinkForm( exchange, 500, "Unexpected failure" );
    }
  }

}
