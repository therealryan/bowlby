package dev.flowfty.bowlby.model.flow;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BROWSER;
import static dev.flowfty.bowlby.model.BowlbySystem.Actors.USER;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.web.WebSequence;
import com.mastercard.test.flow.util.TaggedGroup;

import dev.flowfty.bowlby.model.BowlbySystem.Actors;

/**
 * Flows that explore the behaviour of the index
 */
public class Index extends EagerModel {
  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup();

  /***/
  public Index() {
    super( MODEL_TAGS );

    Flow index = Creator.build( flow -> flow
        .meta( data -> data
            .description( "index" ) )
        .call( web -> web
            .from( USER )
            .to( BROWSER )
            .request( new WebSequence() )
            .call( html -> html
                .to( Actors.BOWLBY )
                .request( new HttpReq() )
                .response( new HttpRes() ) )
            .response( new WebSequence() ) ) );

    members( flatten( index ) );
  }
}
