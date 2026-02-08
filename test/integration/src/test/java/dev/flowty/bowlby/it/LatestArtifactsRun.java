package dev.flowty.bowlby.it;

import dev.flowty.bowlby.app.github.Entity.Branch;
import dev.flowty.bowlby.app.github.Entity.NamedArtifact;
import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.github.Entity.Run;
import dev.flowty.bowlby.app.github.Entity.Workflow;
import dev.flowty.bowlby.app.github.GithubApiClient;
import java.util.Set;

/**
 * Hits github to find the run and artifact ID of the latest run of the
 * artifacts.yml workflow.
 */
public class LatestArtifactsRun {

  private final long runId;
  private final long artifactAlphaId;
  private final long artifactBetaId;

  public LatestArtifactsRun() {

    GithubApiClient github = new GithubApiClient(
        "https://api.github.com",
        System.getenv( "BOWLBY_GH_AUTH_TOKEN" ) );

    Repository repo = new Repository( "therealryan", "bowlby" );
    Workflow workflow = new Workflow( repo, "artifacts.yml" );
    Branch branch = new Branch( repo, "main" );
    Run latest = github.getLatestRun( workflow, branch );
    Set<NamedArtifact> artifacts = github.getArtifacts( latest );

    runId = Long.parseLong( latest.id() );

    artifactAlphaId = artifacts.stream()
        .filter( a -> "artifact_alpha".equals( a.name() ) )
        .findAny()
        .map( a -> Long.parseLong( a.artifact().id() ) )
        .orElseThrow();

    artifactBetaId = artifacts.stream()
        .filter( a -> "artifact_beta".equals( a.name() ) )
        .findAny()
        .map( a -> Long.parseLong( a.artifact().id() ) )
        .orElseThrow();
  }

  long runId() {
    return runId;
  }

  long artifactAlphaId() {
    return artifactAlphaId;
  }

  long artifactBetaId() {
    return artifactBetaId;
  }
}
