package dev.flowty.bowlby.app.srv;

import java.io.IOException;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Serves a classpath resource
 */
class ResourceHandler implements HttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger( ResourceHandler.class );

  private final String name;
  private final String contentType;

  /**
   * @param name        The resource name
   * @param contentType The content type to set in responses
   */
  public ResourceHandler( String name, String contentType ) {
    this.name = name;
    this.contentType = contentType;
  }

  @Override
  public void handle( HttpExchange exchange ) throws IOException {
    try {
      LOG.debug( "{}", exchange.getRequestURI() );

      if( !"GET".equalsIgnoreCase( exchange.getRequestMethod() ) ) {
        ServeUtil.showLinkForm( exchange, 501, "Only GET supported" );
        return;
      }
      URLConnection urlc = Server.class.getResource( name ).openConnection();
      exchange.getResponseHeaders()
          .add( "cache-control", "max-age=31536000, immutable" );
      exchange.getResponseHeaders()
          .add( "content-type", contentType );
      exchange.sendResponseHeaders( 200, urlc.getContentLength() );
      ServeUtil.transfer( urlc::getInputStream, exchange::getResponseBody );
    }
    catch( Exception e ) {
      LOG.error( "request handling failure!", e );
      ServeUtil.showLinkForm( exchange, 500, "Unexpected failure" );
    }
  }
}
