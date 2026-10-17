package com.jk.explore.security.httpsspring;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.security.cert.Certificate;
import java.util.List;

/** What a client saw: the status, the headers, the body, and what TLS negotiated. */
record Reply(int status, HttpHeaders headers, String body, String protocol, Certificate serverCertificate) {

    static Reply of(HttpResponse<String> response) {
        var session = response.sslSession();
        return new Reply(response.statusCode(), response.headers(), response.body(),
                session.map(s -> s.getProtocol()).orElse("none"),
                session.map(s -> {
                    try {
                        return s.getPeerCertificates()[0];
                    } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
                        return null;
                    }
                }).orElse(null));
    }

    String header(String name) {
        return headers.firstValue(name).orElse("");
    }

    List<String> all(String name) {
        return headers.allValues(name);
    }
}
