package dev.flowty.bowlby.app;

import org.slf4j.Logger;

import picocli.CommandLine;

/**
 * Application entrypoint
 */
public class Main {
	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( Main.class );

	/**
	 * Application entrypoint
	 *
	 * @param args command-line arguments
	 */
	public static void main( String... args ) {
		Parameters params = new Parameters();
		new CommandLine( params ).parseArgs( args );
		if( params.help() ) {
			CommandLine.usage( params, System.out );
		}
		else {
			LOG.info( "hello world at {}", params.port() );
			Main main = new Main( params );
			main.start();
		}
	}

	private final Parameters parameters;
	private final Server server;
	private final Artifacts artifacts;

	public Main( Parameters parameters ) {
		this.parameters = parameters;
		artifacts = new Artifacts( parameters.authToken() );
		server = new Server( parameters.port() );
	}

	public void start() {
		server.start();
	}
}
