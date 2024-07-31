package dev.flowty.bowlby.app;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

/**
 * Provides the HTTP server by which artifacts are requested and served
 */
public class Server {
  private static final Logger LOG = LoggerFactory.getLogger( Server.class );

  private static final Map<String, String> SUFFIX_CONTENT_TYPES = new TreeMap<>();
  static {
    SUFFIX_CONTENT_TYPES.put( ".html", "text/html; charset=utf-8" );
    SUFFIX_CONTENT_TYPES.put( ".js", "text/javascript" );
    SUFFIX_CONTENT_TYPES.put( ".png", "image/x-png" );
  }

  private final HttpServer server;

  /**
   * Matches on paths to artiufact files, capturing:
   * <ol>
   * <li>Repository owner</li>
   * <li>Repository name</li>
   * <li>Artifact ID</li>
   * <li>Artifact-internal file path</li>
   * </ol>
   */
  static final Pattern ARTIFACT_PATH = Pattern.compile(
      "/artifact/([^/]+)/([^/]+)/([^/]+)(.*)" );

  /**
   * Creates a new server
   *
   * @param port The port that the server should listen on
   */
  @SuppressWarnings("resource")
  public Server( int port ) {
    try {
      server = HttpServer.create( new InetSocketAddress( port ), 0 );
      server.createContext( "/", exchange -> {
        try {
          byte[] body = ("hello world!\n"
              + exchange.getRequestURI()).getBytes( StandardCharsets.UTF_8 );

          String path = exchange.getRequestURI().toString();
          Matcher m = ARTIFACT_PATH.matcher( path );
          if( m.matches() ) {
            body = String.format( "browsing artifact '%s' '%s' '%s' '%s'",
                m.group( 1 ), m.group( 2 ), m.group( 3 ), m.group( 4 ) )
                .getBytes( StandardCharsets.UTF_8 );
          }

          exchange.sendResponseHeaders( 200, body.length );
          try( OutputStream os = exchange.getResponseBody() ) {
            os.write( body );
          }
        }
        catch( Exception e ) {
          LOG.error( "request handling failure!", e );
          exchange.sendResponseHeaders( 500, 0 );
        }
      } );
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
