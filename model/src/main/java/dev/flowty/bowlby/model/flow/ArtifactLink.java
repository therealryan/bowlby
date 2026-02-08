package dev.flowty.bowlby.model.flow;

import com.mastercard.test.flow.Flow;
import com.mastercard.test.flow.builder.Chain;
import com.mastercard.test.flow.builder.Creator;
import com.mastercard.test.flow.builder.Deriver;
import com.mastercard.test.flow.model.EagerModel;
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
 * Flows that explore the browse-this-artifact behaviour
 */
public class ArtifactLink extends EagerModel {

  /***/
  public static final TaggedGroup MODEL_TAGS = new TaggedGroup("chain:artifact")
      .union("200", "302", "artifact");

  /***
   * @param index provides the basis for our form retrieval
   */
  public ArtifactLink(Index index) {
    super(MODEL_TAGS);

    Chain chain = new Chain("artifact");

    Flow get = Deriver.build(index.get,
        flow -> flow
            .meta(data -> data
                .description("form")
                .motivation(
                    """
                        This chain demonstrates the ability to browse artifact files.
                        We start by visiting the bowlby root page, which offers a form that accepts links to github artifacts."""))
            .prerequisite(index.get)
            .removeCall(Interactions.FAVICON),
        chain);

    Flow submit = Creator.build(flow -> flow
            .meta(data -> data
                .description("submit")
                .tags(Tags.add("302", "200", "artifact"))
                .motivation(
                    """
                        Submitting an artifact link via the form.\
                         Bowlby will return a 302 redirect to the url at which the artifact can be browsed"""))
            .prerequisite(get)
            .call(a -> a
                .from(Actors.USER)
                .to(Actors.BROWSER)
                .request(WebMessage.submit(
                    "https://github.com/therealryan/bowlby/actions/runs/" + ArtifactRun.RUN_ID
                        + "/artifacts/" + ArtifactRun.ARTIFACT_BETA_ID))
                .call(b -> b.to(Actors.BOWLBY)
                    .tags(Tags.add("submit"))
                    .request(HttpMessage.chromeRequest("GET", ""
                        + "/?link=https%3A%2F%2Fgithub.com%2Ftherealryan%2Fbowlby%2Factions%2Fruns%2F"
                        + ArtifactRun.RUN_ID + "%2Fartifacts%2F" + ArtifactRun.ARTIFACT_BETA_ID))
                    .response(HttpMessage.redirectResponse(
                        "/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID + "/")))
                .call(b -> b.to(Actors.BOWLBY)
                    .tags(Tags.add("download"))
                    .request(HttpMessage.chromeRequest("GET",
                        "/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID + "/"))
                    .call(c -> c.to(Actors.GITHUB)
                        .request(ApiMessage.request(
                                "/repos/therealryan/bowlby/actions/artifacts/"
                                    + ArtifactRun.BETA_ID + "/zip")
                            .set(ArtifactRun.BETA_ID, 333333)
                            .masking(Unpredictables.RNG, m -> m
                                .replace(ArtifactRun.BETA_ID, "_masked_")))
                        .response(ApiMessage.artifactRedirect()))
                    .call(c -> c.to(Actors.ARTIFACTS)
                        .request(ArtifactMessage.get("/a/very/long/url"))
                        .response(ArtifactMessage.data(Artifact.BETA)))
                    .response(HttpMessage.directoryListing("file.txt", "subdir/")))
                .response(WebMessage.summarise()
                    .set("forms", "")
                    .set("header", "[bowlby](http://[::]:56567/)")
                    .set("lists", ""
                        + "[file.txt](http://[::]:56567/artifacts/therealryan/bowlby/"
                        + ArtifactRun.ARTIFACT_BETA_ID
                        + "/file.txt)\n"
                        + "[subdir/](http://[::]:56567/artifacts/therealryan/bowlby/"
                        + ArtifactRun.ARTIFACT_BETA_ID
                        + "/subdir/)")
                    .set("url",
                        "http://[::]:56567/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID
                            + "/"))),
        chain);

    Flow dir = Creator.build(flow -> flow.meta(data -> data
                .description("dir")
                .motivation("Clicking through to view the subdirectory"))
            .prerequisite(submit)
            .call(a -> a
                .from(Actors.USER)
                .to(Actors.BROWSER)
                .request(WebMessage.clickLink("subdir/"))
                .call(b -> b.to(Actors.BOWLBY)
                    .request(HttpMessage.chromeRequest("GET",
                        "/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID + "/subdir/"))
                    .response(HttpMessage.directoryListing("../", "subfile.txt")))
                .response(WebMessage.summarise()
                    .set("forms", "")
                    .set("header", "[bowlby](http://[::]:56567/)")
                    .set("lists", ""
                        + "[../](http://[::]:56567/artifacts/therealryan/bowlby/"
                        + ArtifactRun.ARTIFACT_BETA_ID + "/)\n"
                        + "[subfile.txt](http://[::]:56567/artifacts/therealryan/bowlby/"
                        + ArtifactRun.ARTIFACT_BETA_ID
                        + "/subdir/subfile.txt)")
                    .set("url",
                        "http://[::]:56567/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID
                            + "/subdir/"))),
        chain);

    Flow file = Creator.build(flow -> flow.meta(data -> data
                .description("file")
                .motivation("Clicking through to view a file"))
            .prerequisite(dir)
            .call(a -> a
                .from(Actors.USER)
                .to(Actors.BROWSER)
                .request(WebMessage.clickLink("subfile.txt"))
                .call(b -> b.to(Actors.BOWLBY)
                    .request(HttpMessage.chromeRequest("GET",
                        "/artifacts/therealryan/bowlby/" + ArtifactRun.ARTIFACT_BETA_ID
                            + "/subdir/subfile.txt"))
                    .response(HttpMessage.textResponse(
                        "This is just a text file in a subdirectory!\n")))
                .response(WebMessage.text()
                    .set("text", "This is just a text file in a subdirectory!"))),
        chain);

    members(flatten(get, submit, dir, file));
  }
}
