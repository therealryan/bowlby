package dev.flowfty.bowlby.model.msg;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.mastercard.test.flow.Message;
import com.mastercard.test.flow.msg.http.HttpMsg;
import com.mastercard.test.flow.msg.http.HttpReq;
import com.mastercard.test.flow.msg.http.HttpRes;
import com.mastercard.test.flow.msg.json.Json;

import dev.flowfty.bowlby.model.BowlbySystem.Unpredictables;

/**
 * Provides github api messages
 */
public class ApiMessage {

  private ApiMessage() {
    // no instances
  }

  public static Message request( String path ) {
    return new HttpReq()
        .set( HttpReq.METHOD, "GET" )
        .set( HttpReq.PATH, path )
        .set( HttpMsg.header( "accept" ), "application/vnd.github+json" )
        .set( HttpMsg.header( "authorization" ), "Bearer _auth_token_" )
        .set( HttpMsg.header( "x-github-api-version" ), "2022-11-28" )
        .masking( Unpredictables.RNG, m -> m
            .delete( Stream.of(
                "connection", "host", "http2-settings", "upgrade", "user-agent" )
                .map( HttpMsg::header ) ) );
  }

  public static Message response( Object... nvp ) {
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
        .retain( populated ) );
    return res;
  }
}
