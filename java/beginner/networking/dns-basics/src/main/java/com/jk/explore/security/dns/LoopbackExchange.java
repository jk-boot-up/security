package com.jk.explore.security.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/** A real DNS exchange over UDP on this machine: a tiny server answers one question, and a client reads the answer. */
final class LoopbackExchange {

    record Result(boolean answered, boolean idRepeated, int address, boolean nameReadableOnTheWire, int queryBytes, int responseBytes) {
    }

    private LoopbackExchange() {
    }

    static Result ask(String name, int id, long ttl, int address) throws IOException, InterruptedException, BadMessageException {
        InetAddress local = InetAddress.getLoopbackAddress();
        try (DatagramSocket server = new DatagramSocket(0, local); DatagramSocket client = new DatagramSocket(0, local)) {
            client.setSoTimeout(5000);
            server.setSoTimeout(5000);
            Thread thread = Thread.ofPlatform().start(() -> {
                try {
                    DatagramPacket in = new DatagramPacket(new byte[512], 512);
                    server.receive(in);
                    byte[] question = java.util.Arrays.copyOf(in.getData(), in.getLength());
                    byte[] reply = DnsMessage.decode(question).answeredWith(ttl, address).encode();
                    server.send(new DatagramPacket(reply, reply.length, in.getAddress(), in.getPort()));
                } catch (IOException | BadMessageException e) {
                    // the client times out and reports no answer
                }
            });
            byte[] query = DnsMessage.query(id, name).encode();
            client.send(new DatagramPacket(query, query.length, local, server.getLocalPort()));
            DatagramPacket in = new DatagramPacket(new byte[512], 512);
            boolean answered = true;
            try {
                client.receive(in);
            } catch (IOException e) {
                answered = false;
            }
            thread.join();
            if (!answered) {
                return new Result(false, false, 0, false, query.length, 0);
            }
            byte[] response = java.util.Arrays.copyOf(in.getData(), in.getLength());
            DnsMessage message = DnsMessage.decode(response);
            boolean readable = new String(query, StandardCharsets.ISO_8859_1).contains(name.split("\\.")[0]);
            return new Result(true, message.id() == id, message.answers().get(0).address(), readable, query.length, response.length);
        }
    }
}
