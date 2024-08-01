package dev.flowty.bowlby.app;

/**
 * The identifier for a single repository on github
 *
 * @param owner the repository owner
 * @param repo  The repository name
 */
public record GitHubRepository(String owner, String repo) {
  /**
   * @return The path from root to this repo's resources
   */
  public String href() {
    return "/" + owner + "/" + repo;
  }
}
