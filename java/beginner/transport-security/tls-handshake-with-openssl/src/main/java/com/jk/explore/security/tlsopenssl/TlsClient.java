package com.jk.explore.security.tlsopenssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/** A TLS client that checks the server's certificate and that its name matches the name the client asked for. */
final class TlsClient {

    record Result(boolean ok, String protocol, String cipherSuite, String reply, String error) {
    }

    private TlsClient() {
    }

    /**
     * @param hostName  the name to check the certificate against
     * @param protocols the TLS versions to offer, or null for the JDK's defaults
     */
    static Result send(SSLContext context, int port, String hostName, String[] protocols, String line) {
        try (Socket plain = new Socket()) {
            plain.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 5000);
            plain.setSoTimeout(5000);
            try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(plain, hostName, port, true)) {
                SSLParameters parameters = socket.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                if (protocols != null) {
                    parameters.setProtocols(protocols);
                }
                socket.setSSLParameters(parameters);
                socket.startHandshake();
                OutputStream out = socket.getOutputStream();
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                String reply = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).readLine();
                return new Result(true, socket.getSession().getProtocol(), socket.getSession().getCipherSuite(), reply, null);
            }
        } catch (IOException | IllegalArgumentException e) {
            return new Result(false, null, null, null, e.getClass().getSimpleName());
        }
    }
}
