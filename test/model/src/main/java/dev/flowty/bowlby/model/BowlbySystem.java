package dev.flowty.bowlby.model;

import com.mastercard.test.flow.Actor;
import com.mastercard.test.flow.Model;
import com.mastercard.test.flow.Unpredictable;
import com.mastercard.test.flow.model.LazyModel;

import dev.flowty.bowlby.model.flow.ArtifactLink;
import dev.flowty.bowlby.model.flow.Index;
import dev.flowty.bowlby.model.flow.LatestFromWorkflow;

/**
 * Models the expected behaviour of the system
 */
public class BowlbySystem {

  /**
   * The components of the system
   */
  public enum Actors implements Actor {
    /**
     * The user, who just wants to browse some artifact contents without having to
     * download zip files
     */
    USER,
    /**
     * The browser that the user clicks at
     */
    BROWSER,
    /**
     * The bowlby instance that the browser hits
     */
    BOWLBY,
    /**
     * The github API host that bowlby talks to
     */
    GITHUB,
    /**
     * The mysterious download host where the artifacts are supplied from
     */
    ARTIFACTS;
  }

  /**
   * Things in the system that cause unpredictable message content
   */
  public enum Unpredictables implements Unpredictable {
    /**
     * Things that are just not interesting to model
     */
    BORING,
    /**
     * Things that are random
     */
    RNG,
  }

  /**
   * The system model that drives testing
   */
  public static final Model MODEL = new LazyModel( "Bowlby" )
      .with( Index.class )
      .with( LatestFromWorkflow.class )
      .with( ArtifactLink.class )

  ;
}
