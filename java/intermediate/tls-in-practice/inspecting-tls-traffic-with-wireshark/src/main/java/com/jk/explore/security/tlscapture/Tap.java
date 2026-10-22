package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A tap between a client and a server on loopback: it forwards every byte and writes a capture file of what went through. It sees what a machine on the path sees,
 * which is the encrypted records and nothing more.
 */
final class Tap implements AutoCloseable {

    private static final AtomicInteger NEXT_CLIENT_PORT = new AtomicInteger(50000);

    private final ServerSocket listener;
    private final int serverPort;
    private final Path capture;
    private final Thread thread;

    /** Forwards the first connection to {@code serverPort} and writes {@code capture}. */
    Tap(int serverPort, Path capture) throws IOException {
        this.listener = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        this.serverPort = serverPort;
        this.capture = capture;
        this.thread = Thread.ofPlatform().daemon().start(this::serve);
    }

    int port() {
        return listener.getLocalPort();
    }

    private void serve() {
        try (Socket client = listener.accept(); Socket server = new Socket(InetAddress.getLoopbackAddress(), serverPort); Pcap pcap = new Pcap(capture, NEXT_CLIENT_PORT.getAndIncrement(), serverPort)) {
            Thread up = Thread.ofPlatform().daemon().start(() -> pump(client, server, true, pcap));
            pump(server, client, false, pcap);
            up.join(10_000);
        } catch (IOException | InterruptedException e) {
            // the connection ended, or the tap was closed
        }
    }

    private static void pump(Socket from, Socket to, boolean fromClient, Pcap pcap) {
        byte[] buffer = new byte[4096];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int n;
            while ((n = in.read(buffer)) > 0) {
                pcap.data(fromClient, buffer, n);
                out.write(buffer, 0, n);
                out.flush();
            }
            to.shutdownOutput();
        } catch (IOException e) {
            // the other side hung up
        }
    }

    /** Waits until the connection has ended and the capture file is complete. */
    void awaitCapture() throws InterruptedException {
        thread.join(30_000);
    }

    @Override
    public void close() throws IOException {
        listener.close();
    }
}
