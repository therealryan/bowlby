package dev.flowty.bowlby.app;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BOWLBY;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.RNG;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
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
import com.mastercard.test.flow.msg.http.HttpReq;

import dev.flowfty.bowlby.model.BowlbySystem;
import dev.flowfty.bowlby.model.BowlbySystem.Actors;
import dev.flowty.bowlby.app.cfg.Parameters;
import dev.flowty.bowlby.test.HttpFlow;
import dev.flowty.bowlby.test.MockHost;
import dev.flowty.bowlby.test.TestLog;

/**
 * Exercises bowlby in isolation
 */
@SuppressWarnings("static-method")
class MainTest {

  private static Main app;
  private static URI target;
  private static MockHost github;
  private static final HttpClient HTTP = HttpClient.newBuilder().build();

  /**
   * Starts the bowlby instance and github mock
   *
   * @throws URISyntaxException if we fail to build the target uri
   */
  @BeforeAll
  static void start() throws URISyntaxException {
    github = new MockHost( Actors.GITHUB );
    github.start();
    app = new Main( new Parameters(
        "-p", "0",
        "-g", "http:/" + github.address(),
        "-t", "_auth_token_" ) );
    app.start();
    target = new URI( "http:/" + app.address() );
  }

  /**
   * @return test instances
   */
  @TestFactory
  Stream<DynamicNode> tests() {
    return new Flocessor( "isolation", BowlbySystem.MODEL )
        .system( State.FUL, BOWLBY )
        .masking( BORING, RNG )
        .logs( TestLog.TAIL )
        .reporting( Reporting.FAILURES )
        .behaviour( asrt -> {
          HttpReq request = (HttpReq) asrt.expected().request().child();
          try {
            github.seedResponses( asrt );

            HttpResponse<String> response = HTTP.send(
                HttpFlow.sendable( target, request ),
                BodyHandlers.ofString() );

            asrt.actual()
                .response( HttpFlow.assertable( response ) );
            asrt.assertConsequests( github.captured() );
          }
          catch( Exception e ) {
            fail( e );
          }
        } )
        .tests();
  }

  /**
   * Stops the bowlby instance and the github mock
   */
  @AfterAll
  static void stop() {
    app.stop();
    github.stop();
  }

}
