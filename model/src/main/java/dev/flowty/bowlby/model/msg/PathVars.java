package dev.flowty.bowlby.model.msg;

import com.mastercard.test.flow.msg.http.HttpReq;

/**
 * Path variables that we replace at runtime with the details of the latest run
 */
public class PathVars {

  /**
   * The ID of the latest run of the artifacts workflow
   */
  public static String RUN_ID = HttpReq.path( "run_id" );
  /**
   * The ID of the alpha artifact in the latest artifacts run
   */
  public static String ALPHA_ID = HttpReq.path( "alpha_id" );
  /**
   * The ID of the beta artifact in the latest artifacts run
   */
  public static String BETA_ID = HttpReq.path( "beta_id" );
}
