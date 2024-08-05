package dev.flowty.bowlby.app.srv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.sun.net.httpserver.HttpServer;

import dev.flowty.bowlby.app.github.Artifacts;
import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.github.GithubApiClient;

/**
 * Provides the HTTP server by which artifacts are requested and served
 */
public class Server {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( Server.class );
  private final HttpServer server;

  /**
   * Creates a new server
   *
   * @param port                        The port that the server should listen on
   * @param repos                       The set of repos that we're limited to
   * @param ghClient                    Client for the github api
   * @param artifacts                   Manages artifact downloads
   * @param latestArtifactCacheDuration The minimum time between checks for the
   *                                    latest run of a workflow
   */
  @SuppressWarnings("resource")
  public Server( int port, Set<Repository> repos, GithubApiClient ghClient, Artifacts artifacts,
      Duration latestArtifactCacheDuration ) {
    try {
      server = HttpServer.create( new InetSocketAddress( port ), 0 );
      server.setExecutor( Executors.newCachedThreadPool() );
      server.createContext( "/favicon.ico", new ResourceHandler(
          "/favicon.ico", "image/vnd.microsoft.icon" ) );
      server.createContext( "/artifacts", new ArtifactHandler( repos, artifacts ) );
      server.createContext( "/latest",
          new LatestArtifactHandler( repos, ghClient, latestArtifactCacheDuration ) );
      server.createContext( "/", new LinkHandler() );
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( "Failed to create server", ioe );
    }
  }

  /**
   * Starts the server
   */
  public void start() {
    server.start();
    LOG.info( "started at http:/{}", server.getAddress() );
  }

  /**
   * @return The address that the server is listening on
   */
  public InetSocketAddress address() {
    return server.getAddress();
  }

  /**
   * Stops the server
   */
  public void stop() {
    server.stop( 0 );
    LOG.info( "shut down" );
  }
}
