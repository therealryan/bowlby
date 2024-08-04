package dev.flowty.bowlby.app.srv;

import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import dev.flowty.bowlby.app.github.Artifacts;
import dev.flowty.bowlby.app.github.Entity.Artifact;
import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.html.Html;

/**
 * Handles requests to <code>/artifacts/owner/repo/artifactId</code>, serves
 * github artifact contents
 */
class ArtifactHandler implements HttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger( ArtifactHandler.class );

  /**
   * The set of repos that we allow ourselves to serve from
   */
  private final Set<Repository> repos;
  /**
   * The source of the content we serve
   */
  private final Artifacts artifacts;

  /**
   * @param repos     The set of repos that we allow ourselves to serve from
   * @param artifacts The source of the content we serve
   */
  public ArtifactHandler( Set<Repository> repos, Artifacts artifacts ) {

    this.repos = repos;
    this.artifacts = artifacts;
  }

  @Override
  public void handle( HttpExchange exchange ) throws IOException {
    try {
      LOG.debug( "{}", exchange.getRequestURI() );

      if( !"GET".equalsIgnoreCase( exchange.getRequestMethod() ) ) {
        ServeUtil.showLinkForm( exchange, 501, "Only GET supported" );
        return;
      }
      Deque<String> path = Stream.of( exchange.getRequestURI().getPath().split( "/" ) )
          .filter( e -> !e.isEmpty() )
          .collect( toCollection( ArrayDeque::new ) );

      if( path.size() < 4 ) {
        ServeUtil.showLinkForm( exchange, 404, "insufficient path" );
        return;
      }
      if( !"artifacts".equals( path.poll() ) ) {
        ServeUtil.showLinkForm( exchange, 500, "unexpected root" );
        return;
      }

      Repository repo = new Repository( path.poll(), path.poll() );

      if( !repos.isEmpty() && !repos.contains( repo ) ) {
        // we're limited to particular repos, and that isn't one of them
        ServeUtil.showLinkForm( exchange, 403, "forbidden repository addressed" );
        return;
      }

      handleArtifactRequest( exchange, repo, path );
    }
    catch( Exception e ) {
      LOG.error( "request handling failure!", e );
      ServeUtil.showLinkForm( exchange, 500, "Unexpected failure" );
    }
  }

  private void handleArtifactRequest( HttpExchange exchange, Repository repo,
      Deque<String> path ) throws IOException {

    Artifact artifact = new Artifact( repo, path.poll() );
    Path zip = artifacts.get( artifact );
    if( zip == null ) {
      ServeUtil.showLinkForm( exchange, 404, "No such artifact" );
      return;
    }

    try( FileSystem fs = FileSystems.newFileSystem( zip ) ) {
      Path internal = fs.getPath( "/" );
      while( !path.isEmpty() ) {
        internal = internal.resolve( path.poll() );
      }

      if( Files.isRegularFile( internal ) ) {
        serve( exchange, internal );
      }
      else if( Files.isDirectory( internal ) ) {
        listDirectory( exchange, internal );
      }
      else if( !Files.exists( internal ) ) {
        ServeUtil.showLinkForm( exchange, 404, "No such file!" );
      }
      else {
        throw new IllegalStateException( "Unexpected file state! " + zip + "/" + internal );
      }
    }

  }

  private static void serve( HttpExchange exchange, Path file ) {
    try {
      Optional.ofNullable( Files.probeContentType( file ) )
          .ifPresent( ct -> exchange.getResponseHeaders()
              .add( "content-type", ct ) );
      exchange.getResponseHeaders()
          .add( "cache-control", "max-age=31536000, immutable" );
      exchange.sendResponseHeaders( 200, Files.size( file ) );
      ServeUtil.transfer(
          () -> Files.newInputStream( file ),
          exchange::getResponseBody );
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( ioe );
    }
  }

  private static void listDirectory( HttpExchange exchange, Path directory ) throws IOException {
    if( exchange.getRequestURI().getPath().endsWith( "/" ) ) {
      // list dir
      ServeUtil.respond( exchange, 200, dirIndex( directory ) );
    }
    else {
      // no trailing '/': redirect to the explicit dir list. Relative link behaviour
      // breaks down if we don't have the trailing '/'
      ServeUtil.redirect( exchange, exchange.getRequestURI().getPath() + "/" );
    }
  }

  private static String dirIndex( Path dir ) throws IOException {
    Set<Path> all = new TreeSet<>();
    try( Stream<Path> list = Files.list( dir ) ) {
      list.forEach( all::add );
    }

    BiConsumer<Html, Path> pathLink = ( h, p ) -> {
      String href = dir.relativize( p ).toString() + (Files.isDirectory( p ) ? "/" : "");
      h.a( href, href );
    };
    BiConsumer<Html, Path> linkItem = ( h, p ) -> h.li( i -> pathLink.accept( i, p ) );

    return new Html()
        .head( h -> h
            .title( "bowlby" ) )
        .body( b -> b
            .h1( h -> h
                .a( "/", "bowlby" ) )
            .ul( l -> l
                // link to the parent dir if we're not already at the root
                .conditional( c -> c.li( i -> i.a( "../", "../" ) ) ).on( dir.getNameCount() > 0 )
                .repeat( linkItem ).over( all ) ) )
        .toString();
  }
}
