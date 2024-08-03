package dev.flowty.bowlby.app.github;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.flowty.bowlby.app.github.Entity.Artifact;

/**
 * Encapsulates the cache of artifacts downloaded from github
 */
public class Artifacts {

  private static final Logger LOG = LoggerFactory.getLogger( Artifacts.class );

  private static final Path DOWNLOAD_ROOT = Paths.get(
      System.getProperty( "java.io.tmpdir" ), "bowlby", "github" );

  private final GithubApiClient client;

  /**
   * @param client how to interact with github
   */
  public Artifacts( GithubApiClient client ) {
    this.client = client;
  }

  /**
   * Gets an artifact file path, downloading it if necessary
   *
   * @param artifact The ID of the artifact
   * @return The path to the artifact zip file, or <code>null</code> if it could
   *         not be retrieved
   */
  public Path get( Artifact artifact ) {
    Path destination = DOWNLOAD_ROOT
        .resolve( artifact.repo().owner() )
        .resolve( artifact.repo().repo() )
        .resolve( artifact.id() + ".zip" );

    if( Files.exists( destination ) ) {
      return destination;
    }

    return client.getArtifact( artifact, destination );
  }
}
