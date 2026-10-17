package com.jk.explore.security.tlsopenssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/**
 * A tiny TLS server on the loopback interface. It reads one line, answers "received: " and the line, and closes. Any program that
 * speaks TLS can talk to it, including openssl s_client. Handshakes that fail are counted, not printed.
 */
final class TlsServer implements AutoCloseable {

    private final SSLServerSocket socket;
    private final Thread thread;
    private final AtomicInteger failedHandshakes = new AtomicInteger();
    private final AtomicInteger served = new AtomicInteger();

    /** @param protocols the TLS versions to allow, or null for the JDK's defaults */
    TlsServer(SSLContext context, String[] protocols) throws IOException {
        socket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 5, InetAddress.getLoopbackAddress());
        if (protocols != null) {
            socket.setEnabledProtocols(protocols);
        }
        thread = Thread.ofPlatform().daemon().name("tls-server").start(this::loop);
    }

    int port() {
        return socket.getLocalPort();
    }

    int failedHandshakes() {
        return failedHandshakes.get();
    }

    int served() {
        return served.get();
    }

    private void loop() {
        while (!socket.isClosed()) {
            try (SSLSocket connection = (SSLSocket) socket.accept()) {
                connection.setSoTimeout(5000);
                try {
                    connection.startHandshake();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    String line = in.readLine();
                    OutputStream out = connection.getOutputStream();
                    out.write(("received: " + line + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    served.incrementAndGet();
                } catch (IOException e) {
                    failedHandshakes.incrementAndGet();
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    failedHandshakes.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
