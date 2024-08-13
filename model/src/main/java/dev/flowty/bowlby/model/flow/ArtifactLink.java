package dev.flowty.bowlby.model.flow;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Chain;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.builder.Deriver;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.util.TaggedGroup;
import com.mastercard.test.flow.util.Tags;

import dev.flowty.bowlby.model.BowlbySystem.Actors;
import dev.flowty.bowlby.model.msg.ApiMessage;
import dev.flowty.bowlby.model.msg.ArtifactMessage;
import dev.flowty.bowlby.model.msg.ArtifactMessage.Artifact;
import dev.flowty.bowlby.model.msg.HttpMessage;
import dev.flowty.bowlby.model.msg.WebMessage;
import dev.flowty.bowlby.model.msg.ntr.Interactions;

/**
 * Flows that explore the browse-this-artifact behaviour
 */
public class ArtifactLink extends EagerModel {

  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup( "chain:artifact" )
      .union( "200", "302", "artifact" );

  /***
   * @param index provides the basis for our form retrieval
   */
  public ArtifactLink( Index index ) {
    super( MODEL_TAGS );

    Chain chain = new Chain( "artifact" );

    Flow get = Deriver.build( index.get,
        flow -> flow
            .meta( data -> data
                .description( "form" )
                .motivation(
                    """
                        This chain demonstrates the ability to browse artifact files.
                        We start by visiting the bowlby root page, which offers a form that accepts links to github artifacts.""" ) )
            .prerequisite( index.get )
            .removeCall( Interactions.FAVICON ),
        chain );

    Flow submit = Creator.build( flow -> flow
        .meta( data -> data
            .description( "submit" )
            .tags( Tags.add( "302", "200", "artifact" ) )
            .motivation(
                """
                    Submitting an artifact link via the form.\
                     Bowbly will return a 302 redirect to the url at which the artifact can be browsed""" ) )
        .prerequisite( get )
        .call( a -> a
            .from( Actors.USER )
            .to( Actors.BROWSER )
            .request( WebMessage.submit(
                "https://github.com/therealryan/bowlby/actions/runs/10334399684/artifacts/1798279626" ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "submit" ) )
                .request( HttpMessage.chromeRequest( "GET", ""
                    + "/?link=https%3A%2F%2Fgithub.com%2Ftherealryan%2Fbowlby%2Factions%2Fruns%2F10334399684%2Fartifacts%2F1798279626" ) )
                .response( HttpMessage.redirectResponse(
                    "/artifacts/therealryan/bowlby/1798279626/" ) ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "download" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1798279626/" ) )
                .call( c -> c.to( Actors.GITHUB )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/artifacts/1798279626/zip" ) )
                    .response( ApiMessage.artifactRedirect() ) )
                .call( c -> c.to( Actors.ARTIFACTS )
                    .request( ArtifactMessage.get( "/a/very/long/url" ) )
                    .response( ArtifactMessage.data( Artifact.BETA ) ) )
                .response( HttpMessage.directoryListing( "file.txt", "subdir/" ) ) )
            .response( WebMessage.summarise()
                .set( "forms", "" )
                .set( "header", "[bowlby](http://[::]:56567/)" )
                .set( "lists",
                    """
                        [file.txt](http://[::]:56567/artifacts/therealryan/bowlby/1798279626/file.txt)
                        [subdir/](http://[::]:_masked_/artifacts/therealryan/bowlby/_masked_/subdir/)""" )
                .set( "url", "http://[::]:56567/artifacts/therealryan/bowlby/1798279626/" ) ) ),
        chain );

    Flow dir = Creator.build( flow -> flow.meta( data -> data
        .description( "dir" )
        .motivation( "Clicking through to view the subdirectory" ) )
        .prerequisite( submit )
        .call( a -> a
            .from( Actors.USER )
            .to( Actors.BROWSER )
            .request( WebMessage.clickLink( "subdir/" ) )
            .call( b -> b.to( Actors.BOWLBY )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1798279626/subdir/" ) )
                .response( HttpMessage.directoryListing( "../", "subfile.txt" ) ) )
            .response( WebMessage.summarise()
                .set( "forms", "" )
                .set( "header", "[bowlby](http://[::]:56567/)" )
                .set( "lists",
                    """
                        [../](http://[::]:56567/artifacts/therealryan/bowlby/1798279626/)
                        [subfile.txt](http://[::]:_masked_/artifacts/therealryan/bowlby/_masked_/subdir/subfile.txt)""" )
                .set( "url",
                    "http://[::]:56567/artifacts/therealryan/bowlby/1798279626/subdir/" ) ) ),
        chain );

    Flow file = Creator.build( flow -> flow.meta( data -> data
        .description( "file" )
        .motivation( "Clicking through to view a file" ) )
        .prerequisite( dir )
        .call( a -> a
            .from( Actors.USER )
            .to( Actors.BROWSER )
            .request( WebMessage.clickLink( "subfile.txt" ) )
            .call( b -> b.to( Actors.BOWLBY )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1798279626/subdir/subfile.txt" ) )
                .response( HttpMessage.textResponse(
                    "This is just a text file in a subdirectory!\n" ) ) )
            .response( WebMessage.text()
                .set( "text", "This is just a text file in a subdirectory!" ) ) ),
        chain );

    members( flatten( get, submit, dir, file ) );
  }
}
