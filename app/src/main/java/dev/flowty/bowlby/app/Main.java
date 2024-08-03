package dev.flowty.bowlby.app;

import org.slf4j.Logger;

import dev.flowty.bowlby.app.cfg.Parameters;
import dev.flowty.bowlby.app.github.GithubApiClient;
import dev.flowty.bowlby.app.srv.Server;

/**
 * Application entrypoint
 */
public class Main {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( Main.class );

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
      LOG.info( "Server up at {}", main.server.address() );
    }
  }

  private final Server server;

  /**
   * @param parameters Configuration object
   */
  public Main( Parameters parameters ) {
    GithubApiClient ghClient = new GithubApiClient(
        parameters.githubApiHost(), parameters.authToken() );
    server = new Server( parameters.port(), parameters.repos(), ghClient );
  }

  /**
   * Starts the application
   */
  public void start() {
    server.start();
  }
}
