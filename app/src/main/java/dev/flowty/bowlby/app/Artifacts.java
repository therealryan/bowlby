package dev.flowty.bowlby.app;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the cache of downloaded artifacts
 */
public class Artifacts {

	private static final Logger LOG = LoggerFactory.getLogger( Artifacts.class );

	private static final Path DOWNLOAD_ROOT = Paths.get(
			System.getProperty( "java.io.tmpdir" ),
			"bowlby" );

	private final String authToken;

	/**
	 * @param authToken The authorisation token to use for API requests
	 */
	public Artifacts( String authToken ) {
		this.authToken = authToken;
	}

	private static final HttpClient HTTP = HttpClient.newBuilder().build();

	/**
	 * Gets an artifact file path, downloading it if necessary
	 *
	 * @param owner      The repository owner
	 * @param repository The repository name
	 * @param artifactId The ID of the artifact
	 * @return The path to the artifact zip file, or null if it could not be
	 *         retrieved
	 */
	public Path get( String owner, String repository, String artifactId ) {

		Path destination = DOWNLOAD_ROOT
				.resolve( owner )
				.resolve( repository )
				.resolve( artifactId + ".zip" );

		if( Files.exists( destination ) ) {
			return destination;
		}

		LOG.info( "Downloading artifact {}/{}/{}", owner, repository, artifactId );

		// downloading artifacts is a two-step process.
		// First we hit the API to get the download URL
		try {
			HttpResponse<String> redirect = HTTP.send(
					HttpRequest.newBuilder()
							.GET()
							.uri( new URI( String.format(
									"https://api.github.com/repos/%s/%s/actions/artifacts/%s/zip",
									owner, repository, artifactId ) ) )
							.header( "Accept", "application/vnd.github+json" )
							.header( "Authorization", "Bearer " + authToken )
							.header( "X-GitHub-Api-Version", "2022-11-28" )
							.build(),
					BodyHandlers.ofString() );

			Optional<String> dlUri = redirect.headers().firstValue( "location" );
			if( redirect.statusCode() != 302 && dlUri.isEmpty() ) {
				LOG.warn( "Failed to get download URL {}/{}", redirect.statusCode(), dlUri );
			}
			else {
				LOG.debug( "Downloading from {}", dlUri.get() );
				Files.createDirectories( destination.getParent() );
				// then we hit that download URL
				HttpResponse<Path> dl = HTTP.send(
						HttpRequest.newBuilder()
								.GET()
								.uri( new URI( dlUri.get() ) )
								.build(),
						BodyHandlers.ofFile( destination, CREATE, TRUNCATE_EXISTING, WRITE ) );

				LOG.info( "Downloaded to {}" + dl.body() );

				return dl.body();
			}
		}
		catch( IOException | InterruptedException | URISyntaxException e ) {
			LOG.error( "Failed to download artifact {}/{}/{}", owner, repository, artifactId, e );
		}

		return null;
	}
}
