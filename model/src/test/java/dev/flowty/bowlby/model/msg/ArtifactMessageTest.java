package dev.flowty.bowlby.model.msg;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.flowty.bowlby.model.msg.ArtifactMessage.Artifact;

/**
 * Exercises our test artifacts
 */
@SuppressWarnings("static-method")
class ArtifactMessageTest {

  /**
   * Shows the contents of the alpha artifact
   *
   * @throws IOException on failure
   */
  @Test
  void alpha() throws IOException {
    test( Artifact.ALPHA, 518, """
        ┌─ /page.html
        │ <html>
        │   <head>
        │     <title>website</title>
        │     <script type="text/javascript" src="script.js"></script>
        │   </head>
        │   <body>
        │     <p>Jeepers! A website!</p>
        │   </body>
        │ </html>
        └──────
        ┌─ /script.js
        │ document.addEventListener('DOMContentLoaded', function() {
        │   var p = document.createElement('p');
        │   p.innerHTML = 'With javascript!';
        │   document.getElementsByTagName('body').item(0).appendChild(p);
        │ },false);
        └──────""" );
  }

  /**
   * Shows the contents of the beta artifact
   *
   * @throws IOException on failure
   */
  @Test
  void beta() throws IOException {
    test( Artifact.BETA, 327, """
        ┌─ /file.txt
        │ This is just a text file!
        └──────
        ┌─ /subdir/subfile.txt
        │ This is just a text file in a subdirectory!
        └──────""" );
  }

  private static void test( Artifact artifact, int length, String dump ) throws IOException {
    byte[] zipped = artifact.zipContent();

    assertEquals( length, zipped.length );

    Path zip = Paths.get( "target/ArtifactMessageTest/" + artifact.name() + ".zip" );
    Files.createDirectories( zip.getParent() );
    Files.write( zip, zipped );

    try( FileSystem fs = FileSystems.newFileSystem( zip ); ) {

      StringBuilder sb = new StringBuilder();

      for( Path root : fs.getRootDirectories() ) {
        try( Stream<Path> files = Files.walk( root ) ) {
          for( Path file : files
              .filter( Files::isRegularFile )
              .collect( toList() ) ) {
            sb.append( "┌─ " ).append( file ).append( "\n" );
            String content = new String( Files.readAllBytes( file ), UTF_8 );
            Stream.of( content.split( "\n" ) )
                .forEach( line -> sb.append( "│ " ).append( line ).append( "\n" ) );
            sb.append( "└──────" ).append( "\n" );
          }
        }
      }

      assertEquals( dump, sb.toString().trim() );
    }
  }
}
