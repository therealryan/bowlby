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
import com.mastercard.test.flow.msg.txt.Text;

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

    /**
     * @param file A file within an artifact
     * @return The file contents
     */
    public byte[] fileContent( String file ) {
      try( ByteArrayOutputStream baos = new ByteArrayOutputStream();
          InputStream is = ArtifactMessage.class.getResourceAsStream(
              "/" + dir + "/" + file ) ) {
        if( is == null ) {
          return null;
        }
        byte[] buff = new byte[1024];
        int read;
        while( (read = is.read( buff )) != -1 ) {
          baos.write( buff, 0, read );
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
                "content-length", "host", "user-agent" )
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

  /**
   * @param artifact    The artifact
   * @param file        A file within the artifact
   * @param contentType The content type of the file
   * @return The file content response
   */
  public static HttpRes file( Artifact artifact, String file, String contentType ) {
    HttpRes res = new HttpRes()
        .set( HttpMsg.VERSION, "HTTP/1.1" )
        .set( HttpMsg.header( "cache-control" ), "max-age=31536000, immutable" );
    byte[] content = artifact.fileContent( file );
    if( content != null ) {
      res.set( HttpRes.STATUS, 200 )
          .set( HttpMsg.header( "content-length" ), content.length )
          .set( HttpMsg.header( "content-type" ), contentType )
          .set( HttpMsg.BODY, new Text( content ) );
    }
    else {
      res.set( HttpRes.STATUS, 404 );
    }

    res.masking( Unpredictables.BORING, m -> m
        .delete( HttpMsg.header( "date" ) ) );

    return res;
  }
}
