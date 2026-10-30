package org.example.integration;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** A tiny stand-in for sampleServer that returns whatever status and body a test sets. */
final class StubPetsServer implements AutoCloseable {

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String body = "[]";
    private volatile String lastPath;

    StubPetsServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            lastPath = exchange.getRequestURI().getPath();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void respondWith(int status, String body) {
        this.status = status;
        this.body = body;
    }

    String lastPath() {
        return lastPath;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
