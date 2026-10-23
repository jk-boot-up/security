package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A TLS server that is openssl s_server on 127.0.0.1, with or without a stapled OCSP response. */
final class StaplingServer implements AutoCloseable {

    private final Process process;
    private final int port;

    /** The staple may be null: the server then sends no status. */
    StaplingServer(Openssl ssl, Pki pki, Pki.Issued cert, Path staple) throws IOException, InterruptedException {
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            this.port = s.getLocalPort();
        }
        List<String> cmd = new ArrayList<>(ssl.command("s_server", "-accept", "127.0.0.1:" + port, "-cert", cert.certFile().toString(),
                "-key", cert.keyFile().toString(), "-cert_chain", pki.intermediateFile().toString(), "-www", "-quiet"));
        if (staple != null) {
            cmd.addAll(List.of("-status_file", staple.toString()));
        }
        this.process = Sh.start(cmd);
        for (int i = 0; ; i++) {
            if (!process.isAlive()) {
                throw new IllegalStateException("s_server stopped at start");
            }
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException notYet) {
                if (i > 100) {
                    throw new IllegalStateException("s_server did not listen");
                }
                Thread.sleep(100);
            }
        }
    }

    int port() {
        return port;
    }

    @Override
    public void close() throws InterruptedException {
        process.destroy();
        process.waitFor();
    }
}
