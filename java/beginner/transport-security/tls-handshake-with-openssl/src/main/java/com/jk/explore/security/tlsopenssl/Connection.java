package com.jk.explore.security.tlsopenssl;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

/**
 * Two real TLS engines wired together in memory. Nothing touches the network, but every byte one engine produces is exactly what would
 * cross the wire, and is recorded in {@link Wire}.
 */
final class Connection {

    final SSLEngine client;
    final SSLEngine server;
    final Wire wire = new Wire();
    private ByteBuffer toServer = ByteBuffer.allocate(0);
    private ByteBuffer toClient = ByteBuffer.allocate(0);
    private String failure;

    Connection(SSLContext clientContext, SSLContext serverContext, String hostName) {
        client = clientContext.createSSLEngine(hostName, 443);
        client.setUseClientMode(true);
        SSLParameters parameters = client.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        client.setSSLParameters(parameters);
        server = serverContext.createSSLEngine();
        server.setUseClientMode(false);
    }

    /** Runs the handshake until both sides are finished or one fails. Returns whether it succeeded. */
    boolean handshake() {
        try {
            client.beginHandshake();
            boolean progress = true;
            while (progress && failure == null) {
                progress = step(client, true);
                progress |= step(server, false);
            }
        } catch (SSLException e) {
            failure = e.getClass().getSimpleName();
            return false;
        }
        return failure == null
                && client.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                && server.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                && client.getSession().getCipherSuite() != null && !client.getSession().getCipherSuite().contains("NULL");
    }

    private boolean step(SSLEngine engine, boolean isClient) throws SSLException {
        boolean progress = false;
        while (true) {
            switch (engine.getHandshakeStatus()) {
                case NEED_TASK -> {
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    progress = true;
                }
                case NEED_WRAP -> {
                    ByteBuffer out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                    engine.wrap(ByteBuffer.allocate(0), out);
                    out.flip();
                    deliver(isClient, out);
                    progress = true;
                }
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                    ByteBuffer in = isClient ? toClient : toServer;
                    if (!in.hasRemaining() && engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                        return progress;
                    }
                    ByteBuffer plain = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                    SSLEngineResult result = engine.unwrap(in, plain);
                    if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        return progress;
                    }
                    progress = true;
                }
                default -> {
                    // a server that has not started yet is NOT_HANDSHAKING until the first record arrives
                    ByteBuffer in = isClient ? toClient : toServer;
                    if (!in.hasRemaining()) {
                        return progress;
                    }
                    ByteBuffer plain = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                    SSLEngineResult result = engine.unwrap(in, plain);
                    if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        return progress;
                    }
                    progress = true;
                }
            }
        }
    }

    private void deliver(boolean fromClient, ByteBuffer out) {
        byte[] bytes = new byte[out.remaining()];
        out.get(bytes);
        if (fromClient) {
            wire.clientSent(bytes);
            toServer = append(toServer, bytes);
        } else {
            wire.serverSent(bytes);
            toClient = append(toClient, bytes);
        }
    }

    private static ByteBuffer append(ByteBuffer existing, byte[] bytes) {
        ByteBuffer merged = ByteBuffer.allocate(existing.remaining() + bytes.length);
        merged.put(existing).put(bytes).flip();
        return merged;
    }

    String failure() {
        return failure;
    }

    String protocol() {
        return client.getSession().getProtocol();
    }

    String cipherSuite() {
        return client.getSession().getCipherSuite();
    }

    /** Encrypts application data on the client side and returns the record as it would cross the wire. */
    byte[] clientSeals(byte[] data) throws SSLException {
        ByteBuffer out = ByteBuffer.allocate(client.getSession().getPacketBufferSize());
        client.wrap(ByteBuffer.wrap(data), out);
        out.flip();
        byte[] record = new byte[out.remaining()];
        out.get(record);
        return record;
    }

    /** Decrypts a record on the server side. Throws if it was changed. */
    byte[] serverOpens(byte[] record) throws SSLException {
        ByteBuffer plain = ByteBuffer.allocate(server.getSession().getApplicationBufferSize());
        server.unwrap(ByteBuffer.wrap(record), plain);
        plain.flip();
        byte[] data = new byte[plain.remaining()];
        plain.get(data);
        return data;
    }
}
