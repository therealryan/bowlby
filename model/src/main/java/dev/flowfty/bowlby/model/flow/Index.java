package dev.flowfty.bowlby.model.flow;

import static dev.flowfty.bowlby.model.BowlbySystem.Actors.BROWSER;
import static dev.flowfty.bowlby.model.BowlbySystem.Actors.USER;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.util.TaggedGroup;
import com.mastercard.test.flow.util.Tags;

import dev.flowfty.bowlby.model.BowlbySystem.Actors;
import dev.flowfty.bowlby.model.msg.HttpMessage;
import dev.flowfty.bowlby.model.msg.WebMessage;

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
            .request( WebMessage.index() )
            .call( html -> html
                .to( Actors.BOWLBY )
                .tags( Tags.add( "page" ) )
                .request( HttpMessage.chromeRequest( "GET", "/" ) )
                .response( HttpMessage.bowlbyResponse( 200 ) ) )
            .call( html -> html
                .to( Actors.BOWLBY )
                .tags( Tags.add( "icon" ) )
                .request( HttpMessage.chromeRequest( "GET", "/favicon.ico" ) )
                .response( HttpMessage.iconResponse() ) )
            .response( WebMessage.dumpPage() ) ) );

    members( flatten( index ) );
  }
}
