package dev.flowty.bowlby.app.srv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpHandler;

/**
 * Mechanism for behaviours that are interested in server activity
 */
public class ServerListeners {

  private final List<Consumer<Boolean>> listeners = new ArrayList<>();

  /**
   * Wraps a handler in activity-notification behaviour
   *
   * @param handler The handler
   * @return a handler that will notify listeners on behaviour
   */
  public HttpHandler wrap( HttpHandler handler ) {
    return exchange -> {
      try( Notifying n = notifying() ) {
        handler.handle( exchange );
      }
    };
  }

  /**
   * @param listener will be supplied with <code>true</code> when request handling
   *                 begins starts, and <code>false</code> when it ends
   * @return <code>this</code>
   */
  public ServerListeners with( Consumer<Boolean> listener ) {
    listeners.add( listener );
    return this;
  }

  /**
   * @return an {@link AutoCloseable} that handles listener notification
   */
  private Notifying notifying() {
    return new Notifying();
  }

  /**
   * Handles listener notification
   */
  private class Notifying implements AutoCloseable {
    private Notifying() {
      listeners.forEach( l -> l.accept( true ) );
    }

    @Override
    public void close() {
      listeners.forEach( l -> l.accept( false ) );
    }
  }
}
