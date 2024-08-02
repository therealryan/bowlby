package dev.flowty.bowlby.app;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Provides the HTTP server by which artifacts are requested and served
 */
public class Server implements HttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger( Server.class );

  private static final Map<String, String> SUFFIX_CONTENT_TYPES = new TreeMap<>();
  static {
    SUFFIX_CONTENT_TYPES.put( ".html", "text/html; charset=utf-8" );
    SUFFIX_CONTENT_TYPES.put( ".js", "text/javascript" );
    SUFFIX_CONTENT_TYPES.put( ".png", "image/x-png" );
  }

  private final HttpServer server;
  private final Set<GitHubRepository> repos;
  private final Artifacts artifacts;

  /**
   * Creates a new server
   *
   * @param port      The port that the server should listen on
   * @param repos     The set of repos that we're limited to
   * @param artifacts Access to artifact zips
   */
  @SuppressWarnings("resource")
  public Server( int port, Set<GitHubRepository> repos, Artifacts artifacts ) {
    this.repos = repos;
    this.artifacts = artifacts;
    try {
      server = HttpServer.create( new InetSocketAddress( port ), 0 );
      server.createContext( "/", this ); // calls to handle()
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( "Failed to create server", ioe );
    }
    server.setExecutor( Executors.newCachedThreadPool() );
  }

  @Override
  public void handle( HttpExchange exchange ) throws IOException {
    LOG.debug( "handling {} {}", exchange.getRequestMethod(), exchange.getRequestURI() );

    if( !"GET".equalsIgnoreCase( exchange.getRequestMethod() ) ) {
      respond( exchange, 501, "Only GET supported" );
      return;
    }

    try {
      Deque<String> path = Stream.of( exchange.getRequestURI().getPath().split( "/" ) )
          .filter( e -> !e.isEmpty() )
          .collect( toCollection( ArrayDeque::new ) );
      LOG.debug( "path {}", path );

      if( path.isEmpty() ) {
        handleOwnerRequest( exchange );
        return;
      }
      if( path.size() == 1 ) {
        if( "favicon.ico".equals( path.getFirst() ) ) {
          handleFaviconRequest( exchange );
          return;
        }
        handleRepoRequest( exchange, path.poll() );
        return;
      }

      GitHubRepository repo = new GitHubRepository( path.poll(), path.poll() );

      if( !repos.isEmpty() && !repos.contains( repo ) ) {
        // we're limited to particular repos, and that isn't one of them
        respond( exchange, 403, "Forbidden repository addressed" );
        return;
      }

      if( path.isEmpty() ) {
        handleAspectRequest( exchange, repo );
        return;
      }

      String aspect = path.poll();

      if( "artifacts".equals( aspect ) ) {
        handleArtifactRequest( exchange, repo, path );
      }
      else {
        respond( exchange, 404, "No such aspect '" + aspect + "'" );
      }
    }
    catch( Exception e ) {
      LOG.error( "request handling failure!", e );
      respond( exchange, 500, "Unexpected failure" );
    }
  }

  private void handleFaviconRequest( HttpExchange exchange ) throws IOException {
    URL url = Server.class.getResource( "/favicon.ico" );
    try( InputStream source = url.openStream();
        OutputStream sink = exchange.getResponseBody() ) {
      exchange.getResponseHeaders()
          .add( "cache-control", "max-age=31536000, immutable" );
      exchange.getResponseHeaders()
          .add( "content-type", "image/vnd.microsoft.icon" );
      exchange.sendResponseHeaders( 200, url.openConnection().getContentLength() );
      byte[] buff = new byte[1024 * 64];
      int read;
      while( (read = source.read( buff )) != -1 ) {
        sink.write( buff, 0, read );
      }
    }
  }

  private void handleOwnerRequest( HttpExchange exchange ) throws IOException {
    LOG.debug( "displaying owner list" );
    String title = "Repository owners";
    String listPage = new Html()
        .head( h -> h.title( title ) )
        .body( b -> b
            .h1( title )
            .ul( l -> Artifacts.listOwners().forEach( owner -> l
                .li( i -> i.a( "/" + owner, owner ) ) ) ) )
        .toString();
    respond( exchange, 200, listPage );
  }

  private void handleRepoRequest( HttpExchange exchange, String owner ) throws IOException {
    LOG.debug( "displaying owner list" );
    String title = "Repositories of " + owner;
    String listPage = new Html()
        .head( h -> h.title( title ) )
        .body( b -> b
            .h1( title )
            .ul( l -> Artifacts.listRepos( owner ).forEach( repo -> l
                .li( i -> i.a( "/" + owner + "/" + repo, repo ) ) ) ) )
        .toString();
    respond( exchange, 200, listPage );
  }

  private void handleAspectRequest( HttpExchange exchange, GitHubRepository repo )
      throws IOException {
    LOG.debug( "displaying aspect list" );
    String title = "Repository aspects";
    String listPage = new Html()
        .head( h -> h.title( title ) )
        .body( b -> b
            .h1( title )
            .ul( l -> l
                .li( i -> i.a( repo.href() + "/artifacts", "artifacts" ) ) ) )
        .toString();
    respond( exchange, 200, listPage );
  }

  private void handleArtifactRequest( HttpExchange exchange, GitHubRepository repo,
      Deque<String> path ) throws IOException {
    if( path.isEmpty() ) {
      LOG.debug( "displaying artifact list" );
      // list the zips we have
      String title = "Cached artifacts for " + repo.owner() + "/" + repo.repo();
      String listPage = new Html()
          .head( h -> h.title( title ) )
          .body( b -> b
              .h1( title )
              .ul( l -> Artifacts.listArtifacts( repo ).stream().forEach( art -> l
                  .li( i -> i.a( repo.href() + "/artifacts/" + art, art ) ) ) ) )
          .toString();
      respond( exchange, 200, listPage );
    }
    else if( path.size() == 1 ) {
      // assume it's an artifact id - look for indices in the zip
      Path zip = artifacts.get( repo, path.poll() );
      respond( exchange, 503, "pending index search of " + zip
          + "\n" + ZipAccess.list( zip ) );
    }
    else {
      // it's a path into the zip
      Path zip = artifacts.get( repo, path.poll() );
      boolean served = ZipAccess.content( zip, path, file -> {
        try {
          try( InputStream source = Files.newInputStream( file );
              OutputStream sink = exchange.getResponseBody() ) {
            String fileName = file.getFileName().toString();
            SUFFIX_CONTENT_TYPES.entrySet().stream()
                .filter( e -> fileName.endsWith( e.getKey() ) )
                .forEach( e -> exchange.getResponseHeaders()
                    .add( "content-type", e.getValue() ) );
            exchange.getResponseHeaders()
                .add( "cache-control", "max-age=31536000, immutable" );
            exchange.sendResponseHeaders( 200, Files.size( file ) );
            byte[] buff = new byte[1024 * 64];
            int read;
            while( (read = source.read( buff )) != -1 ) {
              sink.write( buff, 0, read );
            }
          }
        }
        catch( IOException ioe ) {
          throw new UncheckedIOException( ioe );
        }
      } );
      if( !served ) {
        respond( exchange, 404, "" );
      }
    }

  }

  private static void respond( HttpExchange exchange, int status, String body ) throws IOException {
    byte[] bytes = body.getBytes( UTF_8 );
    exchange.sendResponseHeaders( status, bytes.length );
    try( OutputStream os = exchange.getResponseBody() ) {
      os.write( bytes );
    }
  }

  /**
   * @return The address of the server
   */
  public String address() {
    return "http:/" + server.getAddress().toString();
  }

  /**
   * Starts the server
   */
  public void start() {
    server.start();
  }

  /**
   * Stops the server
   */
  public void stop() {
    server.stop( 1 );
  }
}
