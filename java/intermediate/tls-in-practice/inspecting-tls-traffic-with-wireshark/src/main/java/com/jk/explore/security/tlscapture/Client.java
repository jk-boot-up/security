package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The openssl command as a TLS client; with a key log file it writes the secrets of the connection, as a browser does when SSLKEYLOGFILE is set. */
final class Client {

    static final String BODY = "user=ana&password=hunter2";

    private Client() {
    }

    /** Logs in through {@code port} (nginx, or a tap in front of it) and returns the page that came back. */
    static String login(int port, Path root, String protocol, Path keyLog) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("openssl", "s_client", "-connect", "127.0.0.1:" + port, "-servername", "www.shop.localhost", "-verify_hostname", "www.shop.localhost",
                "-CAfile", root.toString(), "-quiet", "-ign_eof", protocol));
        if (keyLog != null) {
            command.addAll(List.of("-keylogfile", keyLog.toString()));
        }
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String request = "POST /login HTTP/1.1\r\nHost: www.shop.localhost\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: " + BODY.length() + "\r\nConnection: close\r\n\r\n" + BODY;
        p.getOutputStream().write(request.getBytes());
        p.getOutputStream().flush();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
        }
        return out;
    }
}
