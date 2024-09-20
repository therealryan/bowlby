package dev.flowty.bowlby.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mastercard.test.flow.Actor;
import com.mastercard.test.flow.assrt.Assertion;
import com.mastercard.test.flow.assrt.Consequests;
import com.mastercard.test.flow.msg.ExposedMasking;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.txt.Text;
import com.mastercard.test.flow.util.Flows;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for an http server in the system
 */
public class MockHost {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( MockHost.class );

  private final Actor actor;
  private final Consequests captured;
  private final HttpServer server;

  private final Deque<HttpRes> responses = new ArrayDeque<>();

  /**
   * @param actor    The system component that is being mocked
   * @param captured Where to put captured requests
   */
  public MockHost( Actor actor, Consequests captured ) {
    this( actor, captured, msg -> {
      // no custom behaviour
    } );
  }

  /**
   * @param actor     The system component that is being mocked
   * @param captured  Where to put captured requests
   * @param customise How to alter responses before returning them
   */
  @SuppressWarnings("resource")
  public MockHost( Actor actor, Consequests captured, Consumer<HttpRes> customise ) {
    this.actor = actor;
    this.captured = captured;
    try {
      server = HttpServer.create( new InetSocketAddress( 0 ), 0 );
      server.setExecutor( Executors.newCachedThreadPool() );
      server.createContext( "/", exchange -> {
        HttpReq req = request( exchange );
        LOG.info( "Capturing request to {}:\n{}", actor, req.assertable() );
        captured.capture( actor, req.content() );

        HttpRes res = responses.poll();
        if( res == null ) {
          LOG.error( "We're run out of responses!" );
        }
        else {
          customise.accept( res );
          LOG.info( "Responding from {}:\n {}", actor, res.assertable() );
        }
        respond( exchange, res );
      } );
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( "Failed to create server", ioe );
    }
  }

  /**
   * Seeds a new set of expected responses and clears captured requests
   *
   * @param asrt The source for the new set of responses
   */
  public void seedResponses( Assertion asrt ) {
    captured.clear();
    responses.clear();

    Flows.interactions( asrt.flow() )
        .filter( ntr -> ntr.responder() == actor )
        .map( ntr -> ntr.response().child() )
        .map( msg -> (HttpRes) msg )
        .forEach( responses::add );

    LOG.debug( "Seeded {} responses for {}", responses.size(), actor );
  }

  private static HttpReq request( HttpExchange exchange ) {
    HttpReq req = new HttpReq()
        .set( HttpReq.METHOD, exchange.getRequestMethod() )
        .set( HttpReq.PATH, exchange.getRequestURI().toString() );

    exchange.getRequestHeaders()
        .forEach( ( name, values ) -> req.set(
            HttpMsg.header( name ),
            values.stream().collect( joining( "," ) ) ) );

    try( InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
      byte[] buff = new byte[1024 * 64];
      int read;
      while( (read = in.read( buff )) != -1 ) {
        baos.write( buff, 0, read );
      }
      req.set( HttpMsg.BODY, new Text( baos.toByteArray() ) );
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( ioe );
    }
    return req;
  }

  private static void respond( HttpExchange exchange, HttpRes res ) {
    try {
      int status;
      byte[] content;
      if( res == null ) {
        status = 404;
        content = "We've run out of expected response content!".getBytes( UTF_8 );
      }
      else {
        status = (int) res.get( HttpRes.STATUS );
        content = res.body()
            .map( ExposedMasking::content )
            .orElse( new byte[0] );
        res.headers().forEach( exchange.getResponseHeaders()::add );
      }

      exchange.sendResponseHeaders( status, content.length );
      try( OutputStream os = exchange.getResponseBody() ) {
        os.write( content );
      }
    }
    catch( IOException ioe ) {
      throw new UncheckedIOException( ioe );
    }
  }

  /**
   * Starts the server
   */
  public void start() {
    server.start();
    LOG.info( "started at http:/{}", server.getAddress() );
  }

  /**
   * @return The address that the server is listening on
   */
  public InetSocketAddress address() {
    return server.getAddress();
  }

  public int port() {
    return address().getPort();
  }

  /**
   * Stops the server
   */
  public void stop() {
    server.stop( 0 );
    LOG.info( "shut down" );
  }
}
