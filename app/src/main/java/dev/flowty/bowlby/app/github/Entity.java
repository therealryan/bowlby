package dev.flowty.bowlby.app.github;

/**
 * Typesafe entities for the Github API
 */
public class Entity {

  private Entity() {
    // no instances
  }

  /**
   * The identifier for a single repository in github
   *
   * @param owner the repository owner
   * @param repo  The repository name
   */
  public record Repository(String owner, String repo) {
    /**
     * @return The path from root to this repo's resources
     */
    public String href() {
      return "/" + owner + "/" + repo;
    }
  }

  /**
   * The identifier for a single workflow
   *
   * @param repo The owning repo
   * @param name The name of the workflow
   */
  public record Workflow(Repository repo, String name) {

  }

  /**
   * The identifier for a single run of a workflow
   *
   * @param flow The owning workflow
   * @param id   The ID of the run
   */
  public record Run(Workflow flow, String id) {
  }

  /**
   * The identifier for a single artifact
   *
   * @param repo The owning repo
   * @param id   The ID of the artifact
   */
  public record Artifact(Repository repo, String id) {
  }

  /**
   * An artifact that has a name
   *
   * @param artifact The artifact ID
   * @param name     The artifact name
   */
  public record NamedArtifact(Artifact artifact, String name) {
  }

  /**
   * The name of a {@link Repository} branch
   *
   * @param name The branch name
   */
  public record Branch(String name) {
  }
}
