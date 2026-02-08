package dev.flowty.bowlby.model.flow;

import com.mastercard.test.flow.msg.http.HttpReq;

/**
 * This is sort of sucky, but we're going to have to maintain this for a while.
 * I don't quite have the mana to automate it yet.
 */
public class ArtifactRun {
  static final long ARTIFACT_ALPHA_ID = 5173044800L;
  static final long ARTIFACT_BETA_ID = 5173044842L;



  public static String RUN_ID = HttpReq.path("run_id");
  public static String ALPHA_ID = HttpReq.path("alpha_id");
  public static String BETA_ID = HttpReq.path("beta_id");
}
