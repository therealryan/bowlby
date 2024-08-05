package dev.flowty.bowlby.test;

import java.nio.file.Paths;

import com.mastercard.test.flow.assrt.log.Tail;

/**
 * Captures the logging produced by our <code>simplelogger.properties</code>
 * config file.
 */
public class TestLog {

  /**
   * Captures test logging for reports
   */
  public static final Tail TAIL = new Tail( Paths.get( "target/log.txt" ),
      "^(?<time>\\S+) \\[\\S+\\] (?<level>\\S+) (?<source>\\S+)" );

}
