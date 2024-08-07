package dev.flowfty.bowlby.model.flow;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Chain;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.builder.Deriver;
import com.mastercard.test.flow.model.EagerModel;
import com.mastercard.test.flow.msg.json.Json;
import com.mastercard.test.flow.util.TaggedGroup;
import com.mastercard.test.flow.util.Tags;

import dev.flowfty.bowlby.model.BowlbySystem.Actors;
import dev.flowfty.bowlby.model.msg.ApiMessage;
import dev.flowfty.bowlby.model.msg.HttpMessage;
import dev.flowfty.bowlby.model.msg.WebMessage;
import dev.flowfty.bowlby.model.msg.ntr.Interactions;

/**
 * Flows that explore the get-latest-artifact behaviour
 */
public class Latest extends EagerModel {

  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup( "200", "404" );

  public Latest( Index index ) {
    super( MODEL_TAGS );

    Chain chain = new Chain( "workflow" );

    Flow get = Deriver.build( index.get,
        flow -> flow
            .meta( data -> data
                .description( "get form" )
                .motivation(
                    """
                        This chain illustrates the generation of stable links to the artifacts of the latest run of a workflow.
                        We start by visiting the bowlby root page, which offers a form that accepts links to github workflows.""" ) )
            .prerequisite( index.get )
            .removeCall( Interactions.FAVICON ),
        chain );

    Flow submit = Creator.build( flow -> flow
        .meta( data -> data
            .description( "submit link" )
            .tags( Tags.add( "latest", "submit", "workflow" ) )
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
                    .response( ApiMessage.response(
                        "default_branch", "main",
                        "license", Json.EMPTY_MAP,
                        "owner", Json.EMPTY_MAP,
                        "permissions", Json.EMPTY_MAP,
                        "topics", Json.EMPTY_LIST ) ) )
                .call( c -> c.to( Actors.GITHUB )
                    .tags( Tags.add( "run" ) )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/workflows/artifacts.yml/runs?branch=main&status=completed&per_page=1" ) )
                    .response( ApiMessage.response(
                        "workflow_runs[0].id", 10249960639L,
                        "workflow_runs[0].actor", Json.EMPTY_MAP,
                        "workflow_runs[0].head_commit.author", Json.EMPTY_MAP,
                        "workflow_runs[0].head_commit.committer", Json.EMPTY_MAP,
                        "workflow_runs[0].head_repository.owner", Json.EMPTY_MAP,
                        "workflow_runs[0].pull_requests", Json.EMPTY_LIST,
                        "workflow_runs[0].referenced_workflows", Json.EMPTY_LIST,
                        "workflow_runs[0].repository.owner", Json.EMPTY_MAP,
                        "workflow_runs[0].triggering_actor", Json.EMPTY_MAP ) ) )
                .call( c -> c.to( Actors.GITHUB )
                    .tags( Tags.add( "artifacts" ) )
                    .request( ApiMessage.request(
                        "/repos/therealryan/bowlby/actions/runs/10249960639/artifacts" ) )
                    .response( ApiMessage.response(
                        "artifacts[0].id", 1776512962,
                        "artifacts[0].name", "artifact_alpha",
                        "artifacts[0].workflow_run", Json.EMPTY_MAP,
                        "artifacts[1].id", 1776513003,
                        "artifacts[1].name", "artifact_beta",
                        "artifacts[1].workflow_run", Json.EMPTY_MAP ) ) )
                .response( HttpMessage.linkChoiceResponse( "artifact_alpha", "artifact_beta" ) ) )
            .response( WebMessage.dumpPage()
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

    members( flatten( get, submit ) );
  }
}
