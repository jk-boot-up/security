package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** One local TLS server, openssl s_server, listening on 127.0.0.1 only. */
final class Server implements AutoCloseable {

    /** The three servers of the lab. */
    enum Profile {
        /** Everything a careless 2012 configuration left on. */
        WEAK("AES128-GCM-SHA256:AES128-SHA:AES256-SHA:DHE-RSA-AES128-SHA:DHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES128-GCM-SHA256:NULL-SHA:@SECLEVEL=0", "TLSv1"),
        /** The configuration a bank would run for TLS 1.2 clients. */
        STRONG("ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-CHACHA20-POLY1305", "TLSv1.2"),
        /** Only TLS 1.3, with the old settings still in its configuration file. */
        TLS13_ONLY("DHE-RSA-AES128-SHA:AES128-GCM-SHA256:@SECLEVEL=0", "TLSv1.3");

        final String ciphers;
        final String minProtocol;

        Profile(String ciphers, String minProtocol) {
            this.ciphers = ciphers;
            this.minProtocol = minProtocol;
        }
    }

    private final Process process;
    private final int port;

    Server(Lab lab, Profile profile) throws IOException, InterruptedException {
        this.port = freePort();
        List<String> cmd = new ArrayList<>(lab.openssl().command("s_server", "-accept", "127.0.0.1:" + port,
                "-cert", lab.cert().toString(), "-key", lab.key().toString(),
                "-cipher", profile.ciphers, "-min_protocol", profile.minProtocol, "-serverpref", "-www", "-quiet"));
        if (profile != Profile.STRONG) {
            cmd.addAll(List.of("-dhparam", lab.dhParams().toString()));
        }
        if (profile == Profile.WEAK) {
            cmd.add("-comp");
        }
        this.process = Sh.start(cmd);
        waitUntilListening();
    }

    int port() {
        return port;
    }

    static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    private void waitUntilListening() throws IOException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!process.isAlive()) {
                throw new IllegalStateException("s_server stopped at start");
            }
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException notYet) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("s_server did not listen");
    }

    @Override
    public void close() throws InterruptedException {
        process.destroy();
        process.waitFor();
    }
}
