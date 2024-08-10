package dev.flowty.bowlby.app;

import static dev.flowty.bowlby.model.BowlbySystem.Actors.BOWLBY;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.RNG;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Consequests;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;

import dev.flowty.bowlby.app.cfg.Parameters;
import dev.flowty.bowlby.model.BowlbySystem;
import dev.flowty.bowlby.model.BowlbySystem.Actors;
import dev.flowty.bowlby.model.msg.ApiMessage;
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
  private static final Consequests consequests = new Consequests();
  private static final MockHost artifacts = new MockHost( Actors.ARTIFACTS, consequests );
  private static final MockHost github = new MockHost( Actors.GITHUB, consequests,
      res -> {
        // update the redirect responses to point to our mocked artifact host
        String loc = (String) res.get( HttpMsg.header( "location" ) );
        if( loc != null ) {
          loc = loc.replace( ApiMessage.ARTIFACTS_HOST, "http:/" + artifacts.address() );
          res.set( HttpMsg.header( "location" ), loc );
        }
      } );
  private static final HttpClient HTTP = HttpClient.newBuilder().build();

  /**
   * Starts the bowlby instance and mock downstream hosts
   *
   * @throws URISyntaxException if we fail to build the target uri
   * @throws IOException        if we fail to clean up the artifact dir
   */
  @BeforeAll
  static void start() throws URISyntaxException, IOException {
    Path dir = Paths.get( "target", "MainTest" );
    if( Files.exists( dir ) ) {
      try( Stream<Path> toDelete = Files.walk( dir ) ) {
        toDelete.map( Path::toFile )
            .sorted( Comparator.reverseOrder() )
            .forEach( File::delete );
      }
    }

    artifacts.start();
    github.start();

    app = new Main( new Parameters(
        "-p", "0",
        "-d", dir.toString(),
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
            artifacts.seedResponses( asrt );
            github.seedResponses( asrt );

            HttpResponse<String> response = HTTP.send(
                HttpFlow.sendable( target, request ),
                BodyHandlers.ofString() );

            asrt.actual()
                .response( HttpFlow.assertable( response ) );
            asrt.assertConsequests( consequests );
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
    artifacts.stop();
  }

}
