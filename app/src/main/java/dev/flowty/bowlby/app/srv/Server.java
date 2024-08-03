package dev.flowty.bowlby.app.srv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import dev.flowty.bowlby.app.github.Artifacts;
import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.github.GithubApiClient;

/**
 * Provides the HTTP server by which artifacts are requested and served
 */
public class Server {
  private final HttpServer server;

  /**
   * Creates a new server
   *
   * @param port     The port that the server should listen on
   * @param repos    The set of repos that we're limited to
   * @param ghClient Client for the github api
   */
  @SuppressWarnings("resource")
  public Server( int port, Set<Repository> repos, GithubApiClient ghClient ) {
    try {
      server = HttpServer.create( new InetSocketAddress( port ), 0 );
      server.createContext( "/favicon.ico", new ResourceHandler( "/favicon.ico" ) );
      server.createContext( "/artifacts", new ArtifactHandler( repos, new Artifacts( ghClient ) ) );
      server.createContext( "/latest", new LatestArtifactHandler( repos, ghClient ) );
      server.createContext( "/", new LinkHandler() );
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( "Failed to create server", ioe );
    }
    server.setExecutor( Executors.newCachedThreadPool() );
  }

  /**
   * @return The address of the server
   */
  public String address() {
    return "http:/" + server.getAddress().toString();
  }

  /**
   * Starts the server
   */
  public void start() {
    server.start();
  }

  /**
   * Stops the server
   */
  public void stop() {
    server.stop( 1 );
  }
}
