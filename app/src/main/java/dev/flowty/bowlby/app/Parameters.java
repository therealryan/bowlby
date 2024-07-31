package dev.flowty.bowlby.app;

import java.util.Optional;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Encapsulates the options that can be set in the environment and then
 * overridden on the commandline
 */
@Command(name = "bowlby", description = "A browsing proxy for github action artifacts")
public class Parameters {

	@Option(names = { "-h", "--help" },
			description = "Prints help and exits")
	private boolean help;

	@Option(names = { "-p", "--port" },
			description = "The port at which to serve artifact contents.\nOverrides environment variable 'BOWLBY_PORT'")
	private int port = Optional.ofNullable( System.getenv( "BOWLBY_PORT" ) )
			.filter( v -> v.matches( "\\d+" ) )
			.map( Integer::parseInt )
			.orElse( 56567 );

	@Option(names = { "-a", "--authToken" },
			description = "The github auth token to present on API requests.\nOverrides environment variable 'BOWLBY_GH_AUTH_TOKEN'")
	private String authToken = System.getenv( "BOWLBY_GH_AUTH_TOKEN" );

	/**
	 * @return <code>true</code> if the help text has been requested
	 */
	public boolean help() {
		return help;
	}

	/**
	 * @return The port at which to serve artifact contents
	 */
	public int port() {
		return port;
	}

	/**
	 * @return The github auth token
	 */
	public String authToken() {
		return authToken;
	}
}
