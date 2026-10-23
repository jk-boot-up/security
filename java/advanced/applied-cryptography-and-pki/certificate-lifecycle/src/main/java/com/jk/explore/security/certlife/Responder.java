package com.jk.explore.security.certlife;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The OCSP responder of the lab: a real "openssl ocsp -index" process answers from the CA's database, behind a small relay on 127.0.0.1
 * that counts the requests and can be switched to answer, to refuse connections (the responder is down), or to accept and never answer (it hangs).
 */
final class Responder implements AutoCloseable {

    enum Mode {
        ANSWER, DOWN, HANG
    }

    private final Pki pki;
    private volatile Process process;
    private volatile int backendPort;
    private final int port;
    private final AtomicInteger requests = new AtomicInteger();
    private volatile ServerSocket listener;
    private volatile Mode mode = Mode.ANSWER;

    /** Reserves the public port first, because the certificates must carry its address before the responder exists. */
    static int reservePort() throws IOException {
        return freePort();
    }

    Responder(Pki pki, int port) throws IOException, InterruptedException {
        this.port = port;
        this.pki = pki;
        startBackend();
        listen();
    }

    /**
     * Starts the openssl process that answers from the CA's database. The database is read once at start, so this is also how a change in it
     * (a revocation) reaches the responder: openssl ocsp does not reread the file.
     */
    private void startBackend() throws IOException, InterruptedException {
        this.backendPort = freePort();
        // A connection that sends nothing makes this responder fail its next request, so readiness is read from its log, never probed.
        Path log = pki.dir().resolve("responder.log");
        Files.deleteIfExists(log);
        this.process = new ProcessBuilder(pki.responderCommand(backendPort)).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        for (int i = 0; !Files.exists(log) || !Files.readString(log).contains("waiting for OCSP client connections"); i++) {
            if (i > 100 || !process.isAlive()) {
                throw new IllegalStateException("the OCSP responder did not start");
            }
            Thread.sleep(100);
        }
    }

    /** Makes the responder read the database again. */
    void reload() throws IOException, InterruptedException {
        Process old = process;
        startBackend();
        old.destroy();
        old.waitFor();
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    int port() {
        return port;
    }

    int requests() {
        return requests.get();
    }

    void resetCount() {
        requests.set(0);
    }

    /** Changes what clients see. DOWN closes the listening socket, so connecting fails at once. */
    void mode(Mode m) throws IOException {
        this.mode = m;
        if (m == Mode.DOWN) {
            ServerSocket l = listener;
            if (l != null) {
                l.close();
                listener = null;
            }
        } else if (listener == null) {
            listen();
        }
    }

    private void listen() throws IOException {
        ServerSocket l = new ServerSocket(port, 20, InetAddress.getLoopbackAddress());
        listener = l;
        Thread.ofVirtual().start(() -> {
            while (!l.isClosed()) {
                try {
                    Socket client = l.accept();
                    Thread.ofVirtual().start(() -> serve(client));
                } catch (IOException closed) {
                    return;
                }
            }
        });
    }

    private void serve(Socket client) {
        try (client) {
            requests.incrementAndGet();
            if (mode == Mode.HANG) {
                client.getInputStream().readAllBytes();
                return;
            }
            try (Socket backend = new Socket(InetAddress.getLoopbackAddress(), backendPort)) {
                Thread up = Thread.ofVirtual().start(() -> pipe(client, backend));
                pipe(backend, client);
                up.join();
            }
        } catch (IOException | InterruptedException ended) {
            // the client or the responder went away
        }
    }

    /** Copies bytes one way until either end closes, then closes both so the other direction ends too. */
    private static void pipe(Socket from, Socket to) {
        try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        } catch (IOException ended) {
            // closed by the other direction
        } finally {
            try {
                from.close();
                to.close();
            } catch (IOException ignored) {
                // already closed
            }
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        ServerSocket l = listener;
        if (l != null) {
            l.close();
        }
        process.destroy();
        process.waitFor();
    }
}
