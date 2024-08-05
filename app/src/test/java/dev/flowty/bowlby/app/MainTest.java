package dev.flowty.bowlby.app;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BOWLBY;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.RNG;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.ExposedMasking;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.txt.Text;

import dev.flowfty.bowlby.model.BowlbySystem;
import dev.flowty.bowlby.app.cfg.Parameters;

/**
 * Exercises bowlby in isolation
 */
@SuppressWarnings("static-method")
class MainTest {

  private static Main app;
  private static final HttpClient HTTP = HttpClient.newBuilder().build();

  /**
   * Starts the bowlby instance
   */
  @BeforeAll
  static void start() {
    app = new Main( new Parameters( "-p", "0" ) );
    app.start();
  }

  /**
   * @return test instances
   */
  @TestFactory
  Stream<DynamicNode> tests() {
    return new Flocessor( "end-to-end", BowlbySystem.MODEL )
        .system( State.FUL, BOWLBY )
        .masking( BORING, RNG )
        .reporting( Reporting.FAILURES )
        .behaviour( asrt -> {
          HttpReq req = (HttpReq) asrt.expected().request().child();
          try {
            HttpResponse<String> response = HTTP.send(
                map( req ),
                BodyHandlers.ofString() );
            asrt.actual()
                .response( content( response ) );
          }
          catch( Exception e ) {
            fail( e );
          }
        } )
        .tests();
  }

  private static HttpRequest map( HttpReq req ) {
    try {
      URI uri = new URI( String.format( "http:/%s%s", app.address(), req.path() ) );
      Builder builder = HttpRequest.newBuilder()
          .uri( uri )
          .method( req.method(),
              BodyPublishers.ofByteArray( req.body()
                  .map( ExposedMasking::content )
                  .orElse( new byte[0] ) ) );

      req.headers().forEach( builder::header );
      return builder.build();
    }
    catch( URISyntaxException e ) {
      throw new IllegalStateException( e );
    }
  }

  private static byte[] content( HttpResponse<String> response ) {
    HttpRes res = new HttpRes();
    res.set( HttpRes.STATUS, response.statusCode() );
    response.headers().map().forEach( ( n, v ) -> res.set(
        HttpMsg.header( n ),
        v.stream().collect( joining( "," ) ) ) );
    res.set( HttpMsg.BODY, new Text( response.body() ) );
    return res.content();
  }

  /**
   * Stops the bowlby instance
   */
  @AfterAll
  static void stop() {
    app.stop();
  }

}
