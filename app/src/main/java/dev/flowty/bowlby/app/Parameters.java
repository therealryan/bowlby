package dev.flowty.bowlby.app;

import java.util.Optional;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Encapsulates the options that can be set in the environment and then
 * overridden on the commandline
 */
@Command(name = "bowlby", description = "A browsable proxy for github action artifacts")
public class Parameters {

  @Option(names = { "-h", "--help" },
      description = "Prints help and exits")
  private boolean help;

  @Option(names = { "-p", "--port" }, description = """
      The port at which to serve artifact contents.
      Defaults to 56567
      Overrides environment variable 'BOWLBY_PORT'""")
  private int port = Optional.ofNullable( System.getenv( "BOWLBY_PORT" ) )
      .filter( v -> v.matches( "\\d+" ) )
      .map( Integer::parseInt )
      .orElse( 56567 );

  @Option(names = { "-g", "--ghHost" }, description = """
      The hostname to target with github API requests.
      Defaults to 'https://api.github.com'
      Overrides environment variable 'BOWLBY_GH_HOST'""")
  private String githubApiHost = Optional.ofNullable( System.getenv( "BOWLBY_GH_HOST" ) )
      .orElse( "https://api.github.com" );

  @Option(names = { "-a", "--authToken" }, description = """
      The github auth token to present on API requests.
      Overrides environment variable 'BOWLBY_GH_AUTH_TOKEN'""")
  private String authToken = System.getenv( "BOWLBY_GH_AUTH_TOKEN" );

  public Parameters( String... args ) {
    new CommandLine( this ).parseArgs( args );
  }

  /**
   * If help has been requested, print it to stdout
   *
   * @return <code>true</code> if help was given
   */
  public boolean helpGiven() {
    if( help ) {
      CommandLine.usage( this, System.out );
    }
    return help;
  }

  /**
   * @return The port at which to serve artifact contents
   */
  public int port() {
    return port;
  }

  /**
   * @return The github API hostname
   */
  public String githubApiHost() {
    return githubApiHost;
  }

  /**
   * @return The github auth token
   */
  public String authToken() {
    return authToken;
  }
}
