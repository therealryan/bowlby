package dev.flowty.bowlby.model.msg;

import static java.util.stream.Collectors.toSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;

import dev.flowty.bowlby.model.BowlbySystem.Unpredictables;

/**
 * Provides the messages exchanged with the artifact host: GETs and download
 * responses
 */
public class ArtifactMessage {

  private ArtifactMessage() {
    // no instances
  }

  /**
   * The artifacts we use for testing
   */
  public enum Artifact {
    /***/
    ALPHA("artifact_alpha", "page.html", "script.js"),
    /***/
    BETA("artifact_beta", "file.txt", "subdir/subfile.txt");

    private final String dir;
    private final Set<String> files;

    Artifact( String dir, String... files ) {
      this.dir = dir;
      this.files = Collections.unmodifiableSet( Stream.of( files )
          .collect( toSet() ) );
    }

    /**
     * @return The zipped artifact content
     */
    public byte[] zipContent() {
      try( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
        try( ZipOutputStream zos = new ZipOutputStream( baos ); ) {
          for( String file : files ) {
            ZipEntry e = new ZipEntry( file );
            zos.putNextEntry( e );
            try( InputStream in = ArtifactMessage.class.getResourceAsStream(
                "/" + dir + "/" + file ) ) {
              byte[] buff = new byte[1024];
              int read;
              while( (read = in.read( buff )) != -1 ) {
                zos.write( buff, 0, read );
              }
            }
            zos.closeEntry();
          }
        }
        return baos.toByteArray();
      }
      catch( IOException ioe ) {
        throw new UncheckedIOException( ioe );
      }
    }
  }

  /**
   * @param path The request path
   * @return A GET request
   */
  public static HttpReq get( String path ) {
    return new HttpReq()
        .set( HttpReq.METHOD, "GET" )
        .set( HttpReq.PATH, path )
        .masking( Unpredictables.BORING,
            m -> m.delete( Stream.of(
                "host", "user-agent" )
                .map( HttpMsg::header ) ) );
  }

  /**
   * @param artifact The requested artifact
   * @return A download response
   */
  public static HttpRes data( Artifact artifact ) {
    byte[] body = artifact.zipContent();

    return new HttpRes()
        .set( HttpRes.STATUS, 200 )
        .set( HttpMsg.header( "content-length" ), body.length )
        .set( HttpMsg.BODY, new Bytes( body ) );
  }
}
