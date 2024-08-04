package dev.flowty.bowlby.app.srv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

/**
 * Exercises {@link ServeUtil} behaviours
 */
@SuppressWarnings("static-method")
class ServeUtilTest {

  /**
   * Demonstrates query parameter extraction
   */
  @Test
  void queryParams() {
    BiConsumer<String, String> test = ( in, out ) -> {
      try {
        URI uri = new URI( "http://host.com" + in );
        assertEquals( out, ServeUtil.queryParams( uri ).toString(), "for " + in );
      }
      catch( URISyntaxException e ) {
        fail( e );
      }
    };

    test.accept( "", "{}" );
    test.accept( "?a=b", "{a=[b]}" );
    test.accept( "?a=b&c=d", "{a=[b], c=[d]}" );
    test.accept( "?a=b&c=d&a=e", "{a=[b, e], c=[d]}" );
    test.accept( "?%C3%A9nc%C3%B6d%C3%AAd=%C3%A7h%C3%A5rs", "{éncödêd=[çhårs]}" );
  }
}
