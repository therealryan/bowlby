package dev.flowty.bowlby.app.cfg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.flowty.bowlby.app.github.Entity.Repository;
import dev.flowty.bowlby.app.ui.Gui.IconBehaviour;
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

  @Option(names = { "-d", "--dir" }, description = """
      The directory in which to cache artifacts.
      Defaults to 'bowlby' under the system's temp directory.
      Overrides environment variable 'BOWLBY_DIR'""")
  private String cacheDir = Optional.ofNullable( System.getenv( "BOWLBY_DIR" ) )
      .orElse( System.getProperty( "java.io.tmpdir" ) + "/bowlby" );

  @Option(names = { "-g", "--github" }, description = """
      The hostname to target with github API requests.
      Defaults to 'https://api.github.com'
      Overrides environment variable 'BOWLBY_GH_HOST'""")
  private String githubApiHost = Optional.ofNullable( System.getenv( "BOWLBY_GH_HOST" ) )
      .orElse( "https://api.github.com" );

  @Option(names = { "-t", "--token" }, description = """
      The github authorisation token to present on API requests.
      Overrides environment variable 'BOWLBY_GH_AUTH_TOKEN'""")
  private String authToken = System.getenv( "BOWLBY_GH_AUTH_TOKEN" );

  @Option(names = { "-r", "--repos" },
      description = """
          A comma-separated list of 'owner/repo' pairs, identifying the set of repositories that bowlby will serve artifacts from.
          Defaults to empty, which means _all_ repos will be served.
          Overrides environment variable 'BOWLBY_GH_REPOS'""")
  private String repositories = System.getenv( "BOWLBY_GH_REPOS" );

  @Option(names = { "-l", "--latestValidity" },
      description = """
          An ISO-8601 duration string, controlling how long the latest artifact results are cached for.
          Defaults to 'PT10M', which means it could take up to 10 minutes for the link to the latest artifact to reflect the results of a new run.
          Overrides environment variable 'BOWLBY_LATEST_VALIDITY'""")
  private String latestValidity = Optional.ofNullable( System.getenv( "BOWLBY_LATEST_VALIDITY" ) )
      .orElse( "PT10M" );

  @Option(names = { "-a", "--artifactValidity" },
      description = """
          An ISO-8601 duration string, controlling how long an artifact zip is preserved for.
          Defaults to 'P3D', which means a downloaded artifact zip will be deleted 3 days after its most recent access.
          Overrides environment variable 'BOWLBY_ARTIFACT_VALIDITY'""")
  private String artifactValidity = Optional
      .ofNullable( System.getenv( "BOWLBY_ARTIFACT_VALIDITY" ) )
      .orElse( "P3D" );

  @Option(names = { "-i", "--icon" }, description = """
      Controls the system tray icon. Choose from NONE, STATIC or DYNAMIC.
      The dynamic icon will give a visible indication of request-handling activity
      Overrides environment variable 'BOWLBY_ICON'""")
  private String iconBehaviour = Optional
      .ofNullable( System.getenv( "BOWLBY_ICON" ) )
      .orElse( "DYNAMIC" );

  @Option(names = { "-c", "--context" },
      description = """
          Controls the path at which bowlby assumes it is being served.
          If you're accessing bowlbly directly then you can leave this blank (the default), but if you're proxying requests to your webserver at `/some/path` to hit your bowlby instance then you'll want to set this to `/some/path` so that bowlby can issue redirect responses correctly.
          Overrides environment variable 'BOWLBY_CONTEXT_PATH'""")
  private String contextPath = Optional
      .ofNullable( System.getenv( "BOWLBY_CONTEXT_PATH" ) )
      .orElse( "" );

  private static final Pattern REPO_RGX = Pattern.compile( "(\\w+)/(\\w+)" );
  private final Set<Repository> repos;

  /**
   * @param args From the commandline
   */
  public Parameters( String... args ) {
    new CommandLine( this ).parseArgs( args );

    repos = Stream.of( repositories )
        .filter( Objects::nonNull )
        .flatMap( v -> Stream.of( v.split( "," ) ) )
        .map( String::trim )
        .map( REPO_RGX::matcher )
        .filter( Matcher::matches )
        .map( m -> new Repository( m.group( 1 ), m.group( 2 ) ) )
        .collect( Collectors.toSet() );
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
   * @return The directory in which to cache downloaded artifacts
   */
  public Path dir() {
    return Paths.get( cacheDir );
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

  /**
   * @return The set of repositories that we should limit ourselves to
   */
  public Set<Repository> repos() {
    return repos;
  }

  /**
   * @return The maximum time for which the latest artifacts for a workflow are
   *         cached
   */
  public Duration latestArtifactCacheDuration() {
    return Duration.parse( latestValidity );
  }

  /**
   * @return The time after which an unused artifact zip is deleted
   */
  public Duration artifactCacheDuration() {
    return Duration.parse( artifactValidity );
  }

  /**
   * @return The desired icon behaviour
   */
  public IconBehaviour iconBehaviour() {
    return IconBehaviour.from( iconBehaviour );
  }

  /**
   * @return The root path of the bowlby instance
   */
  public String contextPath() {
    return contextPath;
  }
}
