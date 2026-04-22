package com.scms.main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP entrypoint for cloud deployment.
 * The interactive CLI remains available via com.scms.main.Main.
 */
public class DeployServer {

    private static final int DEFAULT_PORT = 10000;

    public static void main(String[] args) throws IOException {
        int port = resolvePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new TextHandler(
                "Student Course Management System is deployed.\n" +
                        "Use the CLI locally for interactive features."
        ));
        server.createContext("/health", new TextHandler("ok"));

        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();

        System.out.println("[DeployServer] Listening on port " + port);
    }

    private static int resolvePort() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_PORT;
        }
    }

    private static class TextHandler implements HttpHandler {
        private final byte[] body;

        private TextHandler(String body) {
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
