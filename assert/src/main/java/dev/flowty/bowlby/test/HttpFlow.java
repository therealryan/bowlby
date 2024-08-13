package dev.flowty.bowlby.test;

import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import com.mastercard.test.flow.msg.ExposedMasking;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.txt.Text;

/**
 * A bridge between the flow data model and {@link HttpClient}.
 */
public class HttpFlow {

  /**
   * Builds a customisable request from flow data
   *
   * @param host The target of the request
   * @param req  The request content
   * @return A populated builder
   */
  public static Builder builder( URI host, HttpReq req ) {
    URI uri = host.resolve( req.path() );
    Builder builder = HttpRequest.newBuilder()
        .uri( uri )
        .method( req.method(),
            BodyPublishers.ofByteArray( req.body()
                .map( ExposedMasking::content )
                .orElse( new byte[0] ) ) );

    req.headers().forEach( builder::header );
    return builder;
  }

  /**
   * Builds a complete request from flow data
   *
   * @param host The target of the request
   * @param req  The request content
   * @return A sendable request
   */
  public static HttpRequest sendable( URI host, HttpReq req ) {
    return builder( host, req ).build();
  }

  /**
   * Converts a response into assertable content
   *
   * @param response The response
   * @return the message content to supply to flow
   */
  public static byte[] assertable( HttpResponse<String> response ) {
    HttpRes res = new HttpRes();
    if( response.version() == Version.HTTP_1_1 ) {
      res.set( HttpMsg.VERSION, "HTTP/1.1" );
    }
    res.set( HttpRes.STATUS, response.statusCode() );
    response.headers().map().forEach( ( n, v ) -> res.set(
        HttpMsg.header( n ),
        v.stream().collect( joining( "," ) ) ) );
    res.set( HttpMsg.BODY, new Text( response.body() ) );
    return res.content();
  }
}
