package dev.flowty.bowlby.app;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

/**
 * Provides the HTTP server by which artifacts are requested and served
 */
public class Server {
	private static final Map<String, String> SUFFIX_CONTENT_TYPES = new TreeMap<>();
	static {
		SUFFIX_CONTENT_TYPES.put( ".html", "text/html; charset=utf-8" );
		SUFFIX_CONTENT_TYPES.put( ".js", "text/javascript" );
		SUFFIX_CONTENT_TYPES.put( ".png", "image/x-png" );
	}

	private final HttpServer server;

	public Server( int port ) {
		try {
			server = HttpServer.create( new InetSocketAddress( port ), 0 );
			server.createContext( "/", exchange -> {
				byte[] body = "hello world!".getBytes( StandardCharsets.UTF_8 );
				exchange.sendResponseHeaders( 200, body.length );
				try( OutputStream os = exchange.getResponseBody() ) {
					os.write( body );
				}
			} );
		}
		catch( IOException ioe ) {
			throw new UncheckedIOException( "Failed to create server", ioe );
		}
		server.setExecutor( new ThreadPoolExecutor(
				8, 16, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>() ) );
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.stop( 1 );
	}
}
