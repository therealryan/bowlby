package dev.flowty.bowlby.app;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Encapsulates the options that can be set on the commandline
 */
@Command(name = "bowlby", mixinStandardHelpOptions = true,
		description = "A browsing proxy for github action artifacts")
public class Parameters {

	@Option(names = { "-p", "--port" }, description = "The port at which to serve artifact contents")
	private int port = 80;

	/**
	 * @return The port at which to serve artifact contents
	 */
	public int port() {
		return port;
	}
}
