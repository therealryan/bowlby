package dev.flowty.bowlby.model.msg.ntr;

import java.util.function.Consumer;

import com.mastercard.test.flow.Message;
import com.mastercard.test.flow.builder.mutable.MutableInteraction;
import com.mastercard.test.flow.util.InteractionPredicate;

import dev.flowty.bowlby.model.BowlbySystem.Actors;

/**
 * Functions for working with interations
 */
public class Interactions {

  private Interactions() {
    // no instances
  }

  /**
   * Identifies interactions with the browser
   */
  public static final InteractionPredicate BROWSER = new InteractionPredicate()
      .to( Actors.BROWSER );

  /**
   * Identifies interactions with bowlby
   */
  public static final InteractionPredicate BOWLBY = new InteractionPredicate()
      .to( Actors.BOWLBY )
      .without( "icon" );
  /**
   * Identifies interactions with bowlby
   */
  public static final InteractionPredicate FAVICON = BOWLBY
      .with( "icon" )
      .without();

  /**
   * Identifies interactions with github
   */
  public static final InteractionPredicate GITHUB = new InteractionPredicate()
      .to( Actors.GITHUB );

  /**
   * Identifies interactions with the artifact host
   */
  public static final InteractionPredicate ARTIFACTS = new InteractionPredicate()
      .to( Actors.ARTIFACTS );

  /**
   * Sets request message fields
   *
   * @param nvp name/value pairs
   * @return A message-building operation
   */
  public static Consumer<MutableInteraction> rq( Object... nvp ) {
    return ntr -> set( ntr.request(), nvp );
  }

  /**
   * Sets response message fields
   *
   * @param nvp name/value pairs
   * @return A message-building operation
   */
  public static Consumer<MutableInteraction> rs( Object... nvp ) {
    return ntr -> set( ntr.response(), nvp );
  }

  private static void set( Message msg, Object... nvp ) {
    for( int i = 0; i < nvp.length - 1; i += 2 ) {
      msg.set( String.valueOf( nvp[i] ), nvp[i + 1] );
    }
  }
}
