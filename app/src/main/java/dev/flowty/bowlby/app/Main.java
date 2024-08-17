package dev.flowty.bowlby.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import dev.flowty.bowlby.app.cfg.Parameters;
import dev.flowty.bowlby.app.github.Artifacts;
import dev.flowty.bowlby.app.github.GithubApiClient;
import dev.flowty.bowlby.app.srv.Server;
import dev.flowty.bowlby.app.ui.Gui;

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
  private final Gui gui;

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
    gui = new Gui( this, parameters.iconBehaviour() );
  }

  /**
   * Starts the application
   */
  public void start() {
    server.start();
    gui.start();
  }

  /**
   * @return The address that the server is listening on
   */
  public URI uri() {
    try {
      return new URI( "http:/" + server.address() );
    }
    catch( URISyntaxException e ) {
      throw new IllegalStateException( "Bad address", e );
    }
  }

  /**
   * Adds an activity listener
   *
   * @param listener the object to be appraised of request-handling activity
   */
  public void withListener( Consumer<Boolean> listener ) {
    server.withListener( listener );
  }

  /**
   * Stops the application
   */
  public void stop() {
    server.stop();
    gui.stop();
  }
}
