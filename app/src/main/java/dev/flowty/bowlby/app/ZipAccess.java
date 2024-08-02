package dev.flowty.bowlby.app;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Exposes the contents of zip files
 */
public class ZipAccess {

  public static String list( Path zip ) throws IOException {
    try( FileSystem fs = FileSystems.newFileSystem( zip ) ) {
      Set<String> paths = new TreeSet<>();
      for( Path root : fs.getRootDirectories() ) {
        Files.walk( root )
            .forEach( p -> paths.add( p.toString() ) );
      }
      return paths.stream()
          .collect( joining( "\n" ) );
    }
  }

  private static final Map<Path, FileSystem> FS_CACHE = new HashMap<>();

  public static boolean content( Path zip, Deque<String> path, Consumer<Path> fileAction )
      throws IOException {
    try( FileSystem fs = FileSystems.newFileSystem( zip ) ) {
      Path p = fs.getPath( path.poll() );
      for( String step : path ) {
        p = p.resolve( step );
      }

      if( Files.exists( p ) ) {
        fileAction.accept( p );
        return true;
      }
    }
    return false;
  }
}
