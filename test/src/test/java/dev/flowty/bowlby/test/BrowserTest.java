package dev.flowty.bowlby.test;

import static dev.flowty.bowlby.model.BowlbySystem.Actors.BROWSER;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.BORING;
import static dev.flowty.bowlby.model.BowlbySystem.Unpredictables.RNG;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.mastercard.test.flow.assrt.AbstractFlocessor.State;
import com.mastercard.test.flow.assrt.Consequests;
import com.mastercard.test.flow.assrt.Reporting;
import com.mastercard.test.flow.assrt.junit5.Flocessor;
import com.mastercard.test.flow.msg.web.WebSequence;

import dev.flowty.bowlby.model.BowlbySystem;
import dev.flowty.bowlby.model.BowlbySystem.Actors;

/**
 * Exercises the browser in isolation
 */
@SuppressWarnings("static-method")
class BrowserTest {

  private static final Consequests consequests = new Consequests();
  private static final MockHost mock = new MockHost( Actors.BOWLBY, consequests );

  /**
   * Starts the mocked bowlby instance
   */
  @BeforeAll
  static void start() {
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
          if( request.get( "bowlby_url" ) != null ) {
            request.set( "bowlby_url", "http://localhost:" + mock.port() );
          }

          WebDriver driver = driver();

          byte[] actionResults = request.process( driver );

          WebSequence results = (WebSequence) asrt.expected().response();
          byte[] response = results.process( driver );

          asrt.actual()
              .request( actionResults )
              .response( response );
          asrt.assertConsequests( consequests );
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
      ChromeOptions options = new ChromeOptions();
      options.addArguments( "--headless=new" );
      _driver = new ChromeDriver( options );
    }
    return _driver;
  }
}
