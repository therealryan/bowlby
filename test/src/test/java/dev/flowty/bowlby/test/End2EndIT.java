package dev.flowty.bowlby.test;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.ARTIFACT_HOST;
import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BOWLBY;
import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BROWSER;
import static dev.flowfty.bowlby.model.BowlbySystem.Actors.GITHUB;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.RNG;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.web.WebSequence;

import dev.flowfty.bowlby.model.BowlbySystem;
import dev.flowty.bowlby.app.Main;
import dev.flowty.bowlby.app.cfg.Parameters;

/**
 * Exercises the complete system, clicking at the browser and hitting github on
 * the backend.
 */
@SuppressWarnings("static-method")
@EnabledIfEnvironmentVariable(named = "BOWLBY_GH_AUTH_TOKEN", matches = ".+",
    disabledReason = "auth token required to exercise github integration")
class End2EndIT {

  private static Main app;

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
        .system( State.FUL, BROWSER, BOWLBY, GITHUB, ARTIFACT_HOST )
        .masking( BORING, RNG )
        .logs( TestLog.TAIL )
        .reporting( Reporting.ALWAYS, "e2e" )
        .behaviour( asrt -> {
          WebSequence request = (WebSequence) asrt.expected().request().child();
          request.set( "bowlby_url", "http:/" + app.address() );

          WebDriver driver = driver();

          byte[] actionResults = request.process( driver );

          WebSequence results = (WebSequence) asrt.expected().response();
          byte[] response = results.process( driver );

          asrt.actual()
              .request( actionResults )
              .response( response );

        } )
        .tests();
  }

  /**
   * Stops the bowlby instance
   */
  @AfterAll
  static void stop() {
    if( _driver != null ) {
      _driver.close();
    }
    app.stop();
  }

  private static WebDriver _driver;

  private static WebDriver driver() {
    if( _driver == null ) {
      _driver = new ChromeDriver();
    }
    return _driver;
  }
}
