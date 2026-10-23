package com.jk.explore.security.mtls;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/** The old way: a plain HTTP service that lets in anything coming from the trusted network (here the loopback range) and asks nothing else. */
final class NetworkGate implements AutoCloseable {

    private final HttpServer server;

    NetworkGate() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            boolean trustedNetwork = exchange.getRemoteAddress().getAddress().isLoopbackAddress();
            byte[] body = (trustedNetwork ? "done" : "outside").getBytes();
            exchange.sendResponseHeaders(trustedNetwork ? 200 : 403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
