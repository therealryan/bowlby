package dev.flowfty.bowlby.model.msg;

import static java.util.stream.Collectors.joining;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.mastercard.test.flow.Message;
import com.mastercard.test.flow.msg.web.WebSequence;

import dev.flowfty.bowlby.model.BowlbySystem.Unpredictables;

/**
 * browser-interaction messages
 */
public class WebMessage {

  private WebMessage() {
    // no instances
  }

  /**
   * @return Gets the bowlby index
   */
  public static Message index() {
    return new WebSequence()
        .set( "bowlby_url", "http://determinedatruntime.com" )
        .operation( "index", ( driver, params ) -> {
          driver.navigate().to( params.get( "bowlby_url" ) );
        } )
        .masking( Unpredictables.RNG, m -> m
            .replace( "bowlby_url", "_masked_" ) );
  }

  /**
   * @return extracts page details
   */
  public static Message dumpPage() {
    return new WebSequence()
        .set( "url", "http://[::]:56567/" )
        .set( "forms", "sending to _masked_/ with inputs [link:text,:submit]" )
        .set( "header", "[bowlby](https://github.com/therealryan/bowlby)" )
        .set( "lists", "" )
        .set( "text", "" )
        .set( "title", "bowlby" )
        .operation( "dump", ( driver, params ) -> {
          params.put( "url", driver.getCurrentUrl() );
          params.put( "title", driver.getTitle() );
          params.put( "header", summarise( driver, "h1" ) );
          params.put( "forms", summarise( driver, "form" ) );
          params.put( "text", summarise( driver, "p" ) );
          params.put( "lists", summarise( driver, "ul" ) );
        } )
        .masking( Unpredictables.RNG, m -> m
            .string( "url", s -> s.replaceAll( "^.*:\\d+(.*)$", "_masked_$1" ) ) );
  }

  private static String summarise( WebDriver driver, String tag ) {
    return driver.findElements( By.tagName( tag ) ).stream()
        .map( e -> summarise( driver, e ) )
        .collect( joining( "\n" ) );
  }

  private static String summariseChildren( WebDriver driver, WebElement parent, String tag ) {
    return parent.findElements( By.tagName( tag ) ).stream()
        .map( child -> summarise( driver, child ) )
        .collect( joining( "\n" ) );
  }

  private static String summarise( WebDriver driver, WebElement e ) {
    if( "form".equals( e.getTagName() ) ) {
      return String.format( "sending to %s with inputs [%s]",
          e.getAttribute( "action" ).replaceAll( "^.*:\\d+(.*)$", "_masked_$1" ),
          e.findElements( By.tagName( "input" ) ).stream()
              .map( input -> input.getAttribute( "name" ) + ":" + input.getAttribute( "type" ) )
              .collect( joining( "," ) ) );
    }
    if( "a".equals( e.getTagName() ) || "a".equals( e.getTagName() ) ) {
      return "[" + e.getText() + "](" + e.getAttribute( "href" ) + ")";
    }
    if( "h1".equals( e.getTagName() ) || "a".equals( e.getTagName() ) ) {
      return summariseChildren( driver, e, "a" );
    }

    return "Add summary behaviour for: " + (String) ((JavascriptExecutor) driver)
        .executeScript( "return arguments[0].outerHTML;", e );
  }
}
