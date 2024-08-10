package dev.flowty.bowlby.model.flow;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Chain;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.builder.Deriver;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.util.TaggedGroup;
import com.mastercard.test.flow.util.Tags;

import dev.flowty.bowlby.model.BowlbySystem.Actors;
import dev.flowty.bowlby.model.BowlbySystem.Unpredictables;
import dev.flowty.bowlby.model.msg.ApiMessage;
import dev.flowty.bowlby.model.msg.ArtifactMessage;
import dev.flowty.bowlby.model.msg.ArtifactMessage.Artifact;
import dev.flowty.bowlby.model.msg.HttpMessage;
import dev.flowty.bowlby.model.msg.WebMessage;
import dev.flowty.bowlby.model.msg.ntr.Interactions;

/**
 * Flows that explore the get-latest-artifact behaviour
 */
public class Latest extends EagerModel {

  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup( "chain:workflow" )
      .union( "200", "300", "303", "artifact", "latest", "workflow" );

  /**
   * @param index The source of the index-get flow
   */
  public Latest( Index index ) {
    super( MODEL_TAGS );

    Chain chain = new Chain( "workflow" );

    Flow get = Deriver.build( index.get,
        flow -> flow
            .meta( data -> data
                .description( "form" )
                .motivation(
                    """
                        This chain illustrates the generation of stable links to the artifacts of the latest run of a workflow.
                        We start by visiting the bowlby root page, which offers a form that accepts links to github workflows.""" ) )
            .prerequisite( index.get )
            .removeCall( Interactions.FAVICON ),
        chain );

    Flow submit = Creator.build( flow -> flow
        .meta( data -> data
            .description( "submit" )
            .tags( Tags.add( "latest", "workflow", "300", "303" ) )
            .motivation(
                """
                    Submitting a workflow link via the form. Bowlby will:
                     1. Redirect to the appropriate handler with the workflow repo details and name in the path
                     1. Query github to find:
                        * The default branch of the repo
                        * The latest run of the workflow on that branch
                        * The artifacts for that run
                     1. Display a [300-status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/300) [redirection page](https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections#special_redirections) with links to those artifacts

                    The displayed links are stable over time - they will always redirect to the artifacts of the most recent run of the default branch.
                    Users can append path elements to these links to address files within artifacts.""" ) )
        .prerequisite( get )
        .call( a -> a
            .from( Actors.USER )
            .to( Actors.BROWSER )
            .request( WebMessage.submit(
                "https://github.com/therealryan/bowlby/actions/workflows/artifacts.yml" ) )
            .call( b -> b
                .to( Actors.BOWLBY ).tags( Tags.add( "submit" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/?link=https%3A%2F%2Fgithub.com%2Ftherealryan%2Fbowlby%2Factions%2Fworkflows%2Fartifacts.yml" ) )
                .response(
                    HttpMessage.redirectResponse( "/latest/therealryan/bowlby/artifacts.yml" ) ) )
            .call( b -> b
                .to( Actors.BOWLBY ).tags( Tags.add( "latest" ) )
                .request( HttpMessage.chromeRequest(
                    "GET", "/latest/therealryan/bowlby/artifacts.yml" ) )
                .call( c -> c.to( Actors.GITHUB )
                    .tags( Tags.add( "branch" ) )
                    .request( ApiMessage.request( "/repos/therealryan/bowlby" ) )
                    .response( ApiMessage.response( "default_branch", "main" ) ) )
                .call( c -> c.to( Actors.GITHUB )
                    .tags( Tags.add( "run" ) )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/workflows/artifacts.yml/runs?branch=main&status=completed&per_page=1" ) )
                    .response( ApiMessage.response(
                        "workflow_runs[0].id", 10249960639L )
                        .masking( Unpredictables.RNG,
                            m -> m.replace( "workflow_runs[0].id", "_masked_" ) ) ) )
                .call( c -> c.to( Actors.GITHUB )
                    .tags( Tags.add( "artifacts" ) )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/runs/10249960639/artifacts" ) )
                    .response( ApiMessage.response(
                        "artifacts[0].id", 1776512962,
                        "artifacts[0].name", "artifact_alpha",
                        "artifacts[1].id", 1776513003,
                        "artifacts[1].name", "artifact_beta" ) ) )
                .response( HttpMessage.linkChoiceResponse( "artifact_alpha", "artifact_beta" ) ) )
            .response( WebMessage.summarise()
                .set( "header", "[bowlby](http://[::]:56567/)" )
                .set( "forms", "" )
                .set( "lists",
                    """
                        [artifact_alpha](http://[::]:33065/latest/therealryan/bowlby/artifacts.yml/artifact_alpha)
                        [artifact_beta](http://[::]:33065/latest/therealryan/bowlby/artifacts.yml/artifact_beta)""" )
                .set( "text",
                    "These stable links will redirect to the latest artifacts for the artifacts.yml workflow on the default branch."
                        + " Feel free to append path components to address files within the artifacts." )
                .set( "url", "http://[::]:56567/latest/therealryan/bowlby/artifacts.yml" ) ) ),
        chain );

    Flow alpha = Creator.build( flow -> flow
        .meta( data -> data
            .description( "alpha" )
            .tags( Tags.add( "latest", "artifact" ) )
            .motivation(
                """
                    1. Clicks on one of the latest-artifact links
                    1. Get redirected to the static link for that artifact
                    1. Downloads the artifact and displays a file listing""" ) )
        .prerequisite( submit )
        .call( a -> a
            .from( Actors.USER )
            .to( Actors.BROWSER )
            .request( WebMessage.clickLink( "artifact_alpha" ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "redirect" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/latest/therealryan/bowlby/artifacts.yml/artifact_alpha" ) )
                .response( HttpMessage.redirectResponse(
                    "/artifacts/therealryan/bowlby/1776512962/" )
                    .set( HttpMsg.header( "cache-control" ),
                        "public; immutable; max-age=599" ) ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "artifact" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1776512962/" ) )
                .call( c -> c.to( Actors.GITHUB )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/artifacts/1776512962/zip" ) )
                    .response( ApiMessage.artifactRedirect() ) )
                .call( c -> c.to( Actors.ARTIFACTS )
                    .request( ArtifactMessage.get( "/a/very/long/url" ) )
                    .response( ArtifactMessage.data( Artifact.ALPHA ) ) )
                .response( HttpMessage.directoryListing( "page.html", "script.js" ) ) )
            .response( WebMessage.summarise()
                .set( "forms", "" )
                .set( "header", "[bowlby](http://[::]:41289/)" )
                .set( "lists",
                    """
                        [page.html](http://[::]:56567/artifacts/therealryan/bowlby/1776512962/page.html)
                        [script.js](http://[::]:56567/artifacts/therealryan/bowlby/1776512962/script.js)""" )
                .set( "url", "http://[::]:56567/artifacts/therealryan/bowlby/1776512962/" ) ) ),
        chain );

    Flow page = Creator.build( flow -> flow
        .meta( data -> data
            .description( "page" )
            .tags( Tags.add( "200" ) )
            .motivation( "Clicking through to view an artifact file." ) )
        .prerequisite( alpha )
        .call( a -> a.from( Actors.USER ).to( Actors.BROWSER )
            .request( WebMessage.clickLink( "page.html" ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "page" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1776512962/page.html" ) )
                .response( ArtifactMessage.file(
                    Artifact.ALPHA, "page.html", "text/html" ) ) )
            .call( b -> b.to( Actors.BOWLBY )
                .tags( Tags.add( "script" ) )
                .request( HttpMessage.chromeRequest( "GET",
                    "/artifacts/therealryan/bowlby/1776512962/script.js" ) )
                .response( ArtifactMessage.file(
                    Artifact.ALPHA, "script.js", "text/javascript" ) ) )
            .response( WebMessage.summarise()
                .set( "forms", "" )
                .set( "header", "" )
                .set( "text", """
                    Jeepers! A website!
                    With javascript!""" )
                .set( "title", "website" )
                .set( "url", ""
                    + "http://[::]:56567/artifacts/therealryan/bowlby/1776512962/page.html" ) ) ),
        chain );

    members( flatten( get, submit, alpha, page ) );
  }
}
