package com.jk.explore.security.httpsnginx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** The demo's certificates and keys, made once by infra/make-test-pki.sh. The password is a demo value and the keys protect nothing. */
final class Pki {

    static final char[] PASSWORD = "demo-password".toCharArray();
    private static final String[] FOR_NGINX = {"root.pem", "shop.crt", "shop.key", "impostor.crt", "impostor.key"};

    private Pki() {
    }

    private static InputStream resource(String name) throws IOException {
        InputStream in = Pki.class.getResourceAsStream("/pki/" + name);
        if (in == null) {
            throw new IOException("missing resource /pki/" + name);
        }
        return in;
    }

    /** Copies the files nginx needs into a folder that will be mounted in the container, readable by the container's user. */
    static void copyForNginx(Path dir) throws IOException {
        for (String name : FOR_NGINX) {
            Path target = dir.resolve(name);
            try (InputStream in = resource(name)) {
                Files.copy(in, target);
            }
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-r--r--"));
        }
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static KeyStore keyStore(String name) throws IOException, GeneralSecurityException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = resource(name)) {
            store.load(in, PASSWORD);
        }
        return store;
    }

    /** A TLS client setup: either the JDK's own trust (which has never heard of the demo root) or just the demo root, with or without Ana's certificate. */
    static SSLContext sslContext(boolean trustDemoRoot, boolean withClientCertificate) throws IOException, GeneralSecurityException {
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustDemoRoot ? keyStore("trust.p12") : (KeyStore) null);
        KeyManagerFactory keys = null;
        if (withClientCertificate) {
            keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(keyStore("client.p12"), PASSWORD);
        }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys == null ? null : keys.getKeyManagers(), trust.getTrustManagers(), null);
        return context;
    }
}
