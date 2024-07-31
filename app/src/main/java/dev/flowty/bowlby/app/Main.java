package dev.flowty.bowlby.app;

import org.slf4j.Logger;

/**
 * Application entrypoint
 */
public class Main {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( Main.class );

  /**
   * Application entrypoint
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

  private final Parameters parameters;
  private final Server server;
  private final Artifacts artifacts;

  public Main( Parameters parameters ) {
    this.parameters = parameters;
    artifacts = new Artifacts( parameters.githubApiHost(), parameters.authToken() );
    server = new Server( parameters.port() );
  }

  public void start() {
    server.start();
  }
}
