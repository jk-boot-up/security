package com.jk.explore.security.httpsnginx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLParameters;

/** A Java HTTPS client that checks the certificate chain and the host name, the way a browser does. */
public final class TlsClient {

    public record Reply(int status, String protocol, HttpClient.Version version, Map<String, String> headers, String body) {
    }

    /** What the client trusts, shows and allows. */
    public record Options(boolean trustDemoRoot, boolean clientCertificate, String[] protocols, String host, Map<String, String> extraHeaders) {

        public static Options trusting() {
            return new Options(true, false, null, "shop.example.test", Map.of());
        }

        public Options withProtocols(String... allowed) {
            return new Options(trustDemoRoot, clientCertificate, allowed, host, extraHeaders);
        }

        public Options withDefaultTrust() {
            return new Options(false, clientCertificate, protocols, host, extraHeaders);
        }

        public Options withHost(String name) {
            return new Options(trustDemoRoot, clientCertificate, protocols, name, extraHeaders);
        }

        public Options withClientCertificate() {
            return new Options(trustDemoRoot, true, protocols, host, extraHeaders);
        }

        public Options withHeader(String name, String value) {
            Map<String, String> headers = new LinkedHashMap<>(extraHeaders);
            headers.put(name, value);
            return new Options(trustDemoRoot, clientCertificate, protocols, host, headers);
        }
    }

    private TlsClient() {
    }

    /** Asks for a path over HTTPS. Throws IOException (an SSLException, usually) if the connection is refused. */
    public static Reply get(int port, String path, Options options) throws IOException, InterruptedException {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
                    .sslContext(Pki.sslContext(options.trustDemoRoot(), options.clientCertificate()));
            if (options.protocols() != null) {
                // the JDK's HTTP client checks the host name on its own; only the allowed protocols are set here
                SSLParameters parameters = new SSLParameters();
                parameters.setProtocols(options.protocols());
                builder.sslParameters(parameters);
            }
            HttpClient client = builder.build();
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("https://" + options.host() + ":" + port + path));
            options.extraHeaders().forEach(request::header);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((name, values) -> headers.put(name, String.join(", ", values)));
            return new Reply(response.statusCode(), response.sslSession().orElseThrow().getProtocol(), response.version(), headers, response.body());
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    /** True if the connection or the request was refused: a failed handshake, or an answer of 400 or more. */
    public static boolean refused(int port, String path, Options options) throws InterruptedException {
        try {
            return get(port, path, options).status() >= 400;
        } catch (IOException e) {
            return true;
        }
    }
}
