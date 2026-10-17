package com.jk.explore.security.tcpip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A real TCP connection on the loopback interface, with a tap that records what crosses it, as a sniffer on the wire would.
 * TCP delivers bytes reliably and in order; it does not hide them.
 */
final class PlaintextTap {

    record Result(boolean connected, boolean serverReceivedAll, String seenOnTheWire) {
    }

    private PlaintextTap() {
    }

    static Result send(String message) throws IOException, InterruptedException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream tap = new ByteArrayOutputStream();
        byte[][] received = new byte[1][];
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Thread thread = Thread.ofPlatform().start(() -> {
                try (Socket connection = server.accept(); InputStream in = connection.getInputStream()) {
                    received[0] = in.readNBytes(data.length);
                    OutputStream out = connection.getOutputStream();
                    out.write(1);
                    out.flush();
                } catch (IOException e) {
                    received[0] = new byte[0];
                }
            });
            boolean connected;
            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort())) {
                connected = client.isConnected();
                OutputStream out = client.getOutputStream();
                tap.write(data);
                out.write(data);
                out.flush();
                client.getInputStream().read();
            }
            thread.join();
            return new Result(connected, new String(received[0], StandardCharsets.UTF_8).equals(message), tap.toString(StandardCharsets.UTF_8));
        }
    }
}
