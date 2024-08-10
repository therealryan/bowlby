package dev.flowty.bowlby.model.flow;

import static dev.flowty.bowlby.model.msg.ntr.Interactions.BROWSER;
import static dev.flowty.bowlby.model.msg.ntr.Interactions.rq;
import static dev.flowty.bowlby.model.msg.ntr.Interactions.rs;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.builder.Deriver;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.util.TaggedGroup;
import com.mastercard.test.flow.util.Tags;

import dev.flowty.bowlby.model.BowlbySystem.Actors;
import dev.flowty.bowlby.model.msg.HttpMessage;
import dev.flowty.bowlby.model.msg.WebMessage;
import dev.flowty.bowlby.model.msg.ntr.Interactions;

/**
 * Flows that explore the behaviour of the index
 */
public class Index extends EagerModel {
  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup()
      .union( "200", "404" );

  /**
   * A flow that gets the bowlby index
   */
  public Flow get;

  /***/
  public Index() {
    super( MODEL_TAGS );

    get = Creator.build( flow -> flow
        .meta( data -> data
            .description( "root" )
            .tags( Tags.add( "200" ) )
            .motivation( "Displaying the root page" ) )
        .call( web -> web
            .from( Actors.USER )
            .to( Actors.BROWSER )
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
            .response( WebMessage.summarise() ) ) );

    Flow notFound = Deriver.build( get, flow -> flow
        .meta( data -> data
            .description( "not found" )
            .tags( Tags.add( "404" ), Tags.remove( "200" ) )
            .motivation( "Requesting a non-existent page" ) )
        .prerequisite( get )
        .update( BROWSER,
            rq( "path", "/no_such_file" ),
            rs( "header", "[bowlby](http://[::]:56567/)",
                "url", "http://[::]:56567/no_such_file" ) )
        .update( Interactions.BOWLBY,
            rq( HttpReq.PATH, "/no_such_file" ),
            rs( HttpRes.STATUS, 404,
                "/html/body/h1/a/@href", "/" ) )
        .removeCall( ntr -> ntr.tags().contains( "icon" ) ) );

    members( flatten( get, notFound ) );
  }
}
