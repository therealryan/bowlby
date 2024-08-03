package dev.flowty.bowlby.app.srv;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import dev.flowty.bowlby.app.html.Html;

/**
 * Generic handler utils
 */
public class ServeUtil {
  private static final Logger LOG = LoggerFactory.getLogger( ServeUtil.class );

  private ServeUtil() {
    // no instance
  }

  /**
   * Copies all bytes from an {@link InputStream} to an {@link OutputStream}
   *
   * @param source How to get the input
   * @param sink   How to get the output
   * @return The total number of bytes transfered
   * @throws IOException on failure
   */
  public static long transfer( InputStreamSource source, OutputStreamSource sink )
      throws IOException {
    long total = 0;
    try( InputStream src = source.get();
        OutputStream snk = sink.get() ) {
      byte[] buff = new byte[1024 * 64];
      int read;
      while( (read = src.read( buff )) != -1 ) {
        snk.write( buff, 0, read );
        total += read;
      }
    }
    return total;
  }

  /**
   * An object that can produce an {@link InputStream}
   */
  @FunctionalInterface
  public interface InputStreamSource {
    /**
     * @return the {@link InputStream}
     * @throws IOException on failure
     */
    InputStream get() throws IOException;
  }

  /**
   * An object that can produce an {@link OutputStream}
   */
  @FunctionalInterface
  public interface OutputStreamSource {
    /**
     * @return the {@link OutputStream}
     * @throws IOException on failure
     */
    OutputStream get() throws IOException;
  }

  /**
   * Drops html content into the response
   *
   * @param exchange The exchange to complete
   * @param status   The response status
   * @param body     The response body
   * @throws IOException on failure
   */
  public static void respond( HttpExchange exchange, int status, String body ) throws IOException {
    byte[] bytes = body.getBytes( UTF_8 );
    exchange.getResponseHeaders()
        .add( "content-type", "text/html; charset=utf-8" );
    exchange.sendResponseHeaders( status, bytes.length );
    try( OutputStream os = exchange.getResponseBody() ) {
      os.write( bytes );
    }
  }

  /**
   * Sends a redirect response
   *
   * @param exchange    The exchange to complete
   * @param destination The redirect destination
   * @throws IOException on failure
   */
  public static void redirect( HttpExchange exchange, String destination ) throws IOException {
    LOG.debug( "redirecting to {}", destination );
    exchange.getResponseHeaders()
        .add( "location", destination );
    exchange.sendResponseHeaders( 303, 0 );
    exchange.getResponseBody().close();
  }

  /**
   * Extracts the query parameters from a URI
   *
   * @param uri The URI
   * @return The query parameter name/values map
   */
  public static Map<String, List<String>> queryParams( URI uri ) {
    Map<String, List<String>> queryParams = new TreeMap<>();
    Stream.of( Optional.ofNullable( uri.getQuery() ).orElse( "" ).split( "&" ) )
        .filter( p -> !p.isEmpty() )
        .map( pair -> pair.split( "=" ) )
        .forEach( pair -> queryParams.computeIfAbsent(
            urlDecode( pair[0] ),
            n -> new ArrayList<>() )
            .add( urlDecode( pair[1] ) ) );
    return queryParams;
  }

  /**
   * @param encoded URL-encoded string
   * @return the decoded string
   */
  public static String urlDecode( String encoded ) {
    try {
      return URLDecoder.decode( encoded, UTF_8.name() );
    }
    catch( UnsupportedEncodingException e ) {
      e.printStackTrace();
      throw new IllegalStateException( "Failed to decode '" + encoded + "'", e );
    }
  }

  /**
   * Serves the default bowlby page
   *
   * @param exchange The exchange to conclude
   * @param status   The status code to set on the response
   * @param message  The error message, or <code>null</code>
   * @throws IOException on failure
   */
  public static void showLinkForm( HttpExchange exchange, int status, String message )
      throws IOException {
    Html page = new Html()
        .head( h -> h
            .title( "bowlby" ) )
        .body( b -> b
            .h1( h -> h
                .a( "https://github.com/therealryan/bowlby", "bowlby" ) )
            .form( f -> f
                .atr( "action", "/" )
                .input( i -> i
                    .atr( "type", "text" )
                    .atr( "id", "link_input" )
                    .atr( "name", "link" )
                    .atr( "placeholder", "github artifact link" ) )
                .input( i -> i
                    .atr( "type", "submit" )
                    .atr( "id", "submit_input" ) ) )
            .optional( Html::p ).of( message ) );
    respond( exchange, status, page.toString() );
  }
}
