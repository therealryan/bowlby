package dev.flowty.bowlby.test;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BROWSER;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowfty.bowlby.model.BowlbySystem.Unpredictables.RNG;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.web.WebSequence;

import dev.flowfty.bowlby.model.BowlbySystem;
import dev.flowfty.bowlby.model.BowlbySystem.Actors;

/**
 * Exercises the browser in isolation
 */
@SuppressWarnings("static-method")
class BrowserIT {

  private static MockHost mock;

  /**
   * Starts the mocked bowlby instance
   */
  @BeforeAll
  static void start() {
    mock = new MockHost( Actors.BOWLBY );
    mock.start();
  }

  /**
   * @return test instances
   */
  @TestFactory
  Stream<DynamicNode> tests() {
    return new Flocessor( "browser", BowlbySystem.MODEL )
        .system( State.LESS, BROWSER )
        .masking( BORING, RNG )
        .logs( TestLog.TAIL )
        .reporting( Reporting.ALWAYS, "browser" )
        .behaviour( asrt -> {
          mock.seedResponses( asrt );

          WebSequence request = (WebSequence) asrt.expected().request().child();
          request.set( "bowlby_url", "http:/" + mock.address() );

          WebDriver driver = driver();

          byte[] actionResults = request.process( driver );

          WebSequence results = (WebSequence) asrt.expected().response();
          byte[] response = results.process( driver );

          asrt.actual()
              .request( actionResults )
              .response( response );
          asrt.assertConsequests( mock.captured() );
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
    mock.stop();
  }

  private static WebDriver _driver;

  private static WebDriver driver() {
    if( _driver == null ) {
      _driver = new ChromeDriver();
    }
    return _driver;
  }
}
