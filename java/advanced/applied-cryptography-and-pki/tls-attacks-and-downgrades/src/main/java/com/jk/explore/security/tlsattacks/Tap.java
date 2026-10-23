package com.jk.explore.security.tlsattacks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A man in the middle on 127.0.0.1: it forwards TCP to a target and records both directions.
 * It can also break any handshake whose ClientHello offers a version above a limit, the way a faulty network or an attacker makes a newer handshake fail.
 */
final class Tap implements AutoCloseable {

    /** What was seen on one connection. */
    record Capture(boolean broken, byte[] clientToServer, byte[] serverToClient) {
    }

    private final ServerSocket listener;
    private final int targetPort;
    private final int maxHelloVersion;
    private final List<Capture> captures = Collections.synchronizedList(new ArrayList<>());
    private final Thread acceptor;

    /** maxHelloVersion is the highest ClientHello version let through (0x0301 is TLS 1.0); use 0xFFFF to break nothing. */
    Tap(int targetPort, int maxHelloVersion) throws IOException {
        this.targetPort = targetPort;
        this.maxHelloVersion = maxHelloVersion;
        this.listener = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
        this.acceptor = Thread.ofPlatform().daemon().name("tap-accept").start(this::acceptLoop);
    }

    int port() {
        return listener.getLocalPort();
    }

    private void acceptLoop() {
        while (!listener.isClosed()) {
            try {
                Socket client = listener.accept();
                Thread.ofVirtual().start(() -> handle(client));
            } catch (IOException closed) {
                return;
            }
        }
    }

    private void handle(Socket client) {
        ByteArrayOutputStream up = new ByteArrayOutputStream();
        ByteArrayOutputStream down = new ByteArrayOutputStream();
        boolean broken = false;
        try (client; Socket server = new Socket(InetAddress.getLoopbackAddress(), targetPort)) {
            InputStream in = client.getInputStream();
            byte[] header = in.readNBytes(5);
            if (header.length < 5) {
                return;
            }
            int len = TlsRecords.u16(header, 3);
            byte[] body = in.readNBytes(len);
            byte[] first = new byte[5 + body.length];
            System.arraycopy(header, 0, first, 0, 5);
            System.arraycopy(body, 0, first, 5, body.length);
            int version = TlsRecords.clientHelloVersion(first);
            if (version > maxHelloVersion) {
                broken = true;
                return;
            }
            up.writeBytes(first);
            server.getOutputStream().write(first);
            Thread back = Thread.ofVirtual().start(() -> pump(server, client, down));
            pump(client, server, up);
            back.join(5000);
        } catch (IOException | InterruptedException e) {
            // a closed connection ends the capture
        } finally {
            captures.add(new Capture(broken, up.toByteArray(), down.toByteArray()));
        }
    }

    private static void pump(Socket from, Socket to, ByteArrayOutputStream record) {
        byte[] buf = new byte[8192];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int n;
            while ((n = in.read(buf)) > 0) {
                synchronized (record) {
                    record.write(buf, 0, n);
                }
                out.write(buf, 0, n);
                out.flush();
            }
            to.shutdownOutput();
        } catch (IOException ended) {
            // either side closed
        }
    }

    /** Waits until n connections have finished and returns what was recorded. */
    List<Capture> await(int n) throws InterruptedException {
        for (int i = 0; i < 100 && captures.size() < n; i++) {
            Thread.sleep(50);
        }
        synchronized (captures) {
            return new ArrayList<>(captures);
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        listener.close();
        acceptor.join(1000);
    }
}
