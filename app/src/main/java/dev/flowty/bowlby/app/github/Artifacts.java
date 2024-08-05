package dev.flowty.bowlby.app.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

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
  private final Duration artifactValidity;

  /**
   * @param client           how to interact with github
   * @param artifactValidity how long an artifact can go without being accessed
   *                         before we delete it
   */
  public Artifacts( GithubApiClient client, Duration artifactValidity ) {
    this.client = client;
    this.artifactValidity = artifactValidity;
  }

  /**
   * Gets an artifact file path, downloading it if necessary
   *
   * @param artifact The ID of the artifact
   * @return The path to the artifact zip file, or <code>null</code> if it could
   *         not be retrieved
   */
  public Path get( Artifact artifact ) {
    purgeStaleFiles();

    Path destination = DOWNLOAD_ROOT
        .resolve( artifact.repo().owner() )
        .resolve( artifact.repo().repo() )
        .resolve( artifact.id() + ".zip" );

    if( Files.exists( destination ) ) {
      return destination;
    }

    return client.getArtifact( artifact, destination );
  }

  /**
   * Deletes all files in the download dir that haven't been accessed recently
   */
  private void purgeStaleFiles() {
    Instant threshold = Instant.now().minus( artifactValidity );

    try( Stream<Path> files = Files.walk( DOWNLOAD_ROOT ) ) {
      files
          .filter( Files::isRegularFile )
          .filter( file -> {
            try {
              return Files.readAttributes( file, BasicFileAttributes.class )
                  .lastAccessTime()
                  .toInstant()
                  .isBefore( threshold );
            }
            catch( IOException e ) {
              LOG.warn( "Failed to determine last access time of " + file, e );
              return false;
            }
          } )
          .forEach( stale -> {
            try {
              Files.delete( stale );
            }
            catch( IOException e ) {
              LOG.error( "Failed to purge stale artifact " + stale, e );
            }
          } );
    }
    catch( Exception e ) {
      LOG.error( "Failed to purge stale artifacts", e );
    }
  }
}
