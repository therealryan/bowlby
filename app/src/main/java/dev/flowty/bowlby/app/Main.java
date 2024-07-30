package dev.flowty.bowlby.app;

import picocli.CommandLine;

/**
 * Application entrypoint
 */
public class Main {

	/**
	 * Application entrypoint
	 *
	 * @param args command-line arguments
	 */
	public static void main( String... args ) {
		Parameters params = new Parameters();
		new CommandLine( params ).parseArgs( args );
		System.out.println( "hello world at " + params.port() );
		Main main = new Main( params );
		main.start();
	}

	private final Parameters parameters;
	private final Server server;

	public Main( Parameters parameters ) {
		this.parameters = parameters;
		server = new Server( parameters.port() );
	}

	public void start() {
		server.start();
	}
}
