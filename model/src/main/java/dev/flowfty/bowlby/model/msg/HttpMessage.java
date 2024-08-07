package dev.flowfty.bowlby.model.msg;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import com.mastercard.test.flow.Message;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.xml.XML;

import dev.flowfty.bowlby.model.BowlbySystem.Unpredictables;

/**
 * A convenient source of http message content
 */
public class HttpMessage {

  private HttpMessage() {
    // no instances
  }

  /**
   * Builds a typical request from chrome
   *
   * @param method request methd
   * @param path   request path
   * @return the message
   */
  public static Message chromeRequest( String method, String path ) {
    return new HttpReq()
        .set( HttpReq.METHOD, method )
        .set( HttpReq.PATH, path )
        .masking( Unpredictables.BORING, m -> m
            .delete( Stream.of(
                "accept", "accept-encoding", "accept-language", "connection", "host", "referer",
                "upgrade-insecure-requests", "user-agent" )
                .map( HttpMsg::header ) ) );
  }

  /**
   * Builds a redirect request
   *
   * @param location the location header
   * @return the message
   */
  public static Message redirectResponse( String location ) {
    return new HttpRes()
        .set( HttpMsg.VERSION, "HTTP/1.1" )
        .set( HttpRes.STATUS, 303 )
        .set( HttpMsg.header( "content-length" ), 0 )
        .set( HttpMsg.header( "location" ), location )
        .masking( Unpredictables.BORING, m -> m
            .delete( HttpMsg.header( "date" ) ) );
  }

  /**
   * Builds the basic bowlby index response page
   *
   * @param status The response status
   * @return The message
   */
  public static Message bowlbyResponse( int status ) {
    return new HttpRes()
        .set( HttpMsg.VERSION, "HTTP/1.1" )
        .set( HttpRes.STATUS, status )
        .set( HttpMsg.header( "content-type" ), "text/html; charset=utf-8" )
        .set( HttpMsg.BODY, new XML() )
        .set( "/html/head/title", "bowlby" )
        .set( "/html/body/h1/a", "bowlby" )
        .set( "/html/body/h1/a/@href", "https://github.com/therealryan/bowlby" )
        .set( "/html/body/form/@action", "/" )
        .set( "/html/body/form/input[0]/@name", "link" )
        .set( "/html/body/form/input[0]/@id", "link_input" )
        .set( "/html/body/form/input[0]/@type", "text" )
        .set( "/html/body/form/input[0]/@size", "50" )
        .set( "/html/body/form/input[0]/@placeholder", "github artifact or workflow link" )
        .set( "/html/body/form/input[1]/@type", "submit" )
        .set( "/html/body/form/input[1]/@id", "submit_input" )
        .masking( Unpredictables.BORING, m -> m
            .delete( Stream.of(
                "content-length", "date" )
                .map( HttpMsg::header ) ) );
  }

  public static Message linkChoiceResponse( String... artifacts ) {
    HttpRes res = new HttpRes()
        .set( HttpMsg.VERSION, "HTTP/1.1" )
        .set( HttpRes.STATUS, 300 )
        .set( HttpMsg.header( "content-type" ), "text/html; charset=utf-8" )
        .set( HttpMsg.BODY, new XML() )
        .set( "/html/head/title", "bowlby" )
        .set( "/html/body/h1/a", "bowlby" )
        .set( "/html/body/h1/a/@href", "/" )
        .set( "/html/body/p",
            """
                These stable links will redirect to the latest artifacts for the artifacts.yml workflow on the default branch.\
                 Feel free to append path components to address files within the artifacts.""" )
        .masking( Unpredictables.BORING, m -> m
            .delete( Stream.of(
                "content-length", "date" )
                .map( HttpMsg::header ) ) );

    for( int i = 0; i < artifacts.length; i++ ) {
      String link = "/latest/therealryan/bowlby/artifacts.yml/" + artifacts[i];
      res.set( "/html/body/ul/li[" + i + "]/a/@href", link );
      res.set( "/html/body/ul/li[" + i + "]/a", artifacts[i] );
    }
    res.set( HttpMsg.header( "link" ), Stream.of( artifacts )
        .map( a -> "</latest/therealryan/bowlby/artifacts.yml/" + a + ">; rel=alternate" )
        .collect( joining( "," ) ) );

    return res;
  }

  /**
   * Builds bowlby's icon response. Note that we're not bothering to model the
   * icon content
   *
   * @return The message
   */
  public static Message iconResponse() {
    return new HttpRes()
        .set( HttpMsg.VERSION, "HTTP/1.1" )
        .set( HttpRes.STATUS, 200 )
        .set( HttpMsg.header( "cache-control" ), "max-age=31536000, immutable" )
        .set( HttpMsg.header( "content-type" ), "image/vnd.microsoft.icon" )
        .set( HttpMsg.BODY,
            new Bytes( "image bytes, which we're not modelling in tests".getBytes( UTF_8 ) ) )
        .masking( Unpredictables.BORING, m -> m
            .replace( "..", "_masked_".getBytes( UTF_8 ) )
            .delete( Stream.of(
                "content-length", "date" )
                .map( HttpMsg::header ) ) );
  }
}
