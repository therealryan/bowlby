package dev.flowty.bowlby.app.srv;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Deals with submitted links
 */
public class LinkHandler implements HttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger( LinkHandler.class );

  private static final Pattern GITHUB_ARTIFACT_LINK = Pattern.compile(
      "https://github.com/([^/]+)/([^/]+)/actions/runs/\\d+/artifacts/(\\d+)" );

  @Override
  public void handle( HttpExchange exchange ) throws IOException {
    try {
      LOG.debug( "{}", exchange.getRequestURI() );
      if( !"GET".equalsIgnoreCase( exchange.getRequestMethod() ) ) {
        ServeUtil.showLinkForm( exchange, 501, "Only GET supported" );
        return;
      }

      Map<String, List<String>> queryParams = ServeUtil.queryParams( exchange.getRequestURI() );

      String link = Optional.of( queryParams )
          .map( m -> m.get( "link" ) )
          .filter( l -> !l.isEmpty() )
          .map( l -> l.get( 0 ) )
          .orElse( null );

      if( link != null ) {
        Matcher m = GITHUB_ARTIFACT_LINK.matcher( link );
        if( m.find() ) {
          ServeUtil.redirect( exchange, String.format(
              "/artifacts/%s/%s/%s",
              m.group( 1 ), m.group( 2 ), m.group( 3 ) ) );
          return;
        }
        ServeUtil.showLinkForm( exchange, 200, "Failed to grok " + link );
        return;
      }

      ServeUtil.showLinkForm( exchange, 200, null );
    }
    catch( Exception e ) {
      LOG.error( "request handling failure!", e );
      ServeUtil.showLinkForm( exchange, 500, "Unexpected failure" );
    }
  }
}
