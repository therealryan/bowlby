package dev.flowty.bowlby.it;

import static dev.flowty.bowlby.model.BowlbySystem.Actors.GITHUB;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.RNG;
import static org.junit.jupiter.api.Assertions.fail;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.http.HttpReq;
import dev.flowty.bowlby.model.BowlbySystem;
import dev.flowty.bowlby.model.msg.PathVars;
import dev.flowty.bowlby.test.HttpFlow;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Exercises github in isolation
 */
@SuppressWarnings("static-method")
@EnabledIfEnvironmentVariable(named = "BOWLBY_GH_AUTH_TOKEN", matches = ".+",
    disabledReason = "auth token required to exercise github integration")
public class ApiIT {

  @SuppressWarnings("resource")
  private static final HttpClient HTTP = HttpClient.newBuilder().build();
  private static final URI TARGET;

  static {
    try {
      TARGET = new URI( "https://api.github.com" );
    }
    catch( URISyntaxException e ) {
      throw new IllegalArgumentException( "Bad github URI", e );
    }
  }

  private static LatestArtifactsRun latest = new LatestArtifactsRun();

  /**
   * @return test instances
   */
  @TestFactory
  Stream<DynamicNode> tests() {
    return new Flocessor( "github", BowlbySystem.MODEL )
        .system( State.FUL, GITHUB )
        .masking( BORING, RNG )
        .reporting( Reporting.ALWAYS, "github" )
        .behaviour( asrt -> {
          HttpReq request = (HttpReq) asrt.expected().request().child();

          try {
            // inject latest run values
            request.set( PathVars.RUN_ID, latest.runId() );
            request.set( PathVars.ALPHA_ID, latest.artifactAlphaId() );
            request.set( PathVars.BETA_ID, latest.artifactBetaId() );

            HttpRequest.Builder req = HttpFlow.builder( TARGET, request );

            // Take careful note of how we're populating the auth token at the last possible
            // moment, not passing the secret value outside of this class, not logging the
            // request, and not populating it into the flow data model
            req.setHeader( "authorization", "Bearer " + System.getenv( "BOWLBY_GH_AUTH_TOKEN" ) );
            // we're trying to minimise the possibility of it leaking to disk

            HttpResponse<String> response = HTTP.send(
                req.build(),
                BodyHandlers.ofString() );

            asrt.actual()
                .request( request.content() )
                .response( HttpFlow.assertable( response ) );
          }
          catch( Exception e ) {
            fail( e );
          }
        } )
        .tests();
  }
}
