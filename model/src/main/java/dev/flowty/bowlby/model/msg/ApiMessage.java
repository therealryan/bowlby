package dev.flowty.bowlby.model.msg;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.json.Json;

import dev.flowty.bowlby.model.BowlbySystem.Unpredictables;

/**
 * Provides github api messages
 */
public class ApiMessage {

  /**
   * The hostname where our github API responses will direct artifact downloads
   */
  public static final String ARTIFACTS_HOST = "https://artifacts.example.com";

  private ApiMessage() {
    // no instances
  }

  /**
   * Creates a typical API request
   *
   * @param path The request path
   * @return A GET request
   */
  public static HttpReq request( String path ) {
    return new HttpReq()
        .set( HttpReq.METHOD, "GET" )
        .set( HttpReq.PATH, path )
        .set( HttpMsg.header( "accept" ), "application/vnd.github+json" )
        .set( HttpMsg.header( "authorization" ), "Bearer _auth_token_" )
        .set( HttpMsg.header( "x-github-api-version" ), "2022-11-28" )
        .masking( Unpredictables.RNG, m -> m
            .delete( Stream.of(
                "connection", "content-length", "host", "http2-settings", "upgrade", "user-agent" )
                .map( HttpMsg::header ) ) );
  }

  /**
   * Creates a typical API response
   *
   * @param nvp name/value pairs for the json body
   * @return A JSON-bearing response
   */
  public static HttpRes response( Object... nvp ) {
    HttpRes res = new HttpRes()
        .set( HttpRes.STATUS, 200 )
        .set( HttpMsg.BODY, new Json() );
    Set<String> populated = new TreeSet<>();
    populated.add( HttpRes.STATUS );
    populated.add( HttpMsg.BODY );

    for( int i = 0; i < nvp.length - 1; i += 2 ) {
      res.set( (String) nvp[i], nvp[i + 1] );
      populated.add( (String) nvp[i] );
    }
    res.masking( Unpredictables.BORING, m -> m
        .retain( populated )
        .delete(
            "license", "owner", "permissions", "topics",
            "workflow_runs[0].actor", "workflow_runs[0].head_commit",
            "workflow_runs[0].head_repository", "workflow_runs[0].pull_requests",
            "workflow_runs[0].referenced_workflows", "workflow_runs[0].repository",
            "workflow_runs[0].triggering_actor",
            "artifacts[0].workflow_run", "artifacts[1].workflow_run" ) );
    return res;
  }

  /**
   * Creates a typical artifact download redirect response
   *
   * @return A 302 response
   */
  public static HttpRes artifactRedirect() {
    return new HttpRes()
        .set( HttpRes.STATUS, 302 )
        .set( HttpMsg.header( "location" ), ARTIFACTS_HOST + "/a/very/long/url" )
        .masking( Unpredictables.BORING, m -> m
            .retain( HttpRes.STATUS,
                HttpMsg.header( "location" ) ) )
        .masking( Unpredictables.RNG, m -> m
            .replace( HttpMsg.header( "location" ), "https://somehost.net/a/very/long/url" ) );
  }
}
