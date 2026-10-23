package com.jk.explore.security.workloadid;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The "before": a ledger that trusts one static secret and the caller's own word about who it is. */
final class LegacyLedger implements AutoCloseable {

    /** The ledger's answer to a call. */
    static int decide(String secret, String presentedKey, String claimedService, String method, String path) {
        if (presentedKey == null || !presentedKey.equals(secret)) {
            return 401;
        }
        // The key proves nothing about which service holds it, so the claimed name is all there is to check.
        return Policy.allows(Policy.DOMAIN + claimedService, method, path) ? 200 : 403;
    }

    private final HttpServer server;
    private volatile String secret;
    private final List<String> audit = new CopyOnWriteArrayList<>();

    LegacyLedger(String secret) throws IOException {
        this.secret = secret;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String claimed = exchange.getRequestHeaders().getFirst("X-Service");
            int code = decide(this.secret, exchange.getRequestHeaders().getFirst("X-Api-Key"), claimed,
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            if (code != 401) {
                audit.add(claimed);
            }
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        });
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    void rotateSecret(String newSecret) {
        this.secret = newSecret;
    }

    /** The caller names the ledger wrote down, in order. */
    List<String> audit() {
        return List.copyOf(audit);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
