package dev.flowty.bowlby.app;

import java.net.InetSocketAddress;

import dev.flowty.bowlby.app.cfg.Parameters;
import dev.flowty.bowlby.app.github.Artifacts;
import dev.flowty.bowlby.app.github.GithubApiClient;
import dev.flowty.bowlby.app.srv.Server;

/**
 * Application entrypoint
 */
public class Main {

  /**
   * Application entry-point
   *
   * @param args command-line arguments
   */
  public static void main( String... args ) {
    Parameters params = new Parameters( args );
    if( params.helpGiven() ) {
      // nothing more to do
    }
    else {
      Main main = new Main( params );
      main.start();
    }
  }

  private final Server server;

  /**
   * @param parameters Configuration object
   */
  public Main( Parameters parameters ) {
    GithubApiClient ghClient = new GithubApiClient(
        parameters.githubApiHost(),
        parameters.authToken() );
    Artifacts artifacts = new Artifacts(
        ghClient,
        parameters.dir(),
        parameters.artifactCacheDuration() );
    server = new Server(
        parameters.port(),
        parameters.repos(),
        ghClient,
        artifacts,
        parameters.latestArtifactCacheDuration() );
  }

  /**
   * Starts the application
   */
  public void start() {
    server.start();
  }

  /**
   * @return The address that the server is listening on
   */
  public InetSocketAddress address() {
    return server.address();
  }

  /**
   * Stops the application
   */
  public void stop() {
    server.stop();
  }
}
