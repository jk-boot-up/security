package com.jk.explore.security.mtls;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

/** Builds the TLS settings of a service: the certificate it shows and the authorities it trusts. */
final class Tls {

    private Tls() {
    }

    /** Always offers the one credential it holds, so a certificate that the other side would not accept is still sent and refused there. */
    private static final class Fixed extends X509ExtendedKeyManager {

        private volatile Credential credential;

        Fixed(Credential credential) {
            this.credential = credential;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[] {"id"};
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return "id";
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return "id";
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[] {"id"};
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return "id";
        }

        @Override
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return "id";
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return credential.chain().toArray(new X509Certificate[0]);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return credential.key();
        }
    }

    /** Passes every decision to the trust manager it holds now, which can be replaced while connections are being made. */
    private static final class Swappable extends X509ExtendedTrustManager {

        private volatile X509ExtendedTrustManager current;

        Swappable(X509ExtendedTrustManager current) {
            this.current = current;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            current.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            current.checkServerTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            current.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            current.checkServerTrusted(chain, authType, engine);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            current.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            current.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return current.getAcceptedIssuers();
        }
    }

    /** A TLS context whose certificate and trusted authorities can be replaced while the service keeps running. */
    static final class Live {

        private final Fixed keys;
        private final Swappable trust;
        private final SSLContext context;

        Live(Credential credential, List<X509Certificate> trusted) throws GeneralSecurityException {
            keys = new Fixed(credential);
            trust = new Swappable(trustManager(trusted));
            context = SSLContext.getInstance("TLS");
            context.init(new KeyManager[] {keys}, new TrustManager[] {trust}, null);
        }

        SSLContext context() {
            return context;
        }

        /** New connections use these from now on. */
        void replace(Credential credential, List<X509Certificate> trusted) throws GeneralSecurityException {
            trust.current = trustManager(trusted);
            keys.credential = credential;
        }
    }

    private static X509ExtendedTrustManager trustManager(List<X509Certificate> trusted) throws GeneralSecurityException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try {
            store.load(null, null);
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
        for (int i = 0; i < trusted.size(); i++) {
            store.setCertificateEntry("root" + i, trusted.get(i));
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(store);
        return (X509ExtendedTrustManager) tmf.getTrustManagers()[0];
    }

    /** credential may be null (no client certificate); trusted are the root certificates this side accepts. */
    static SSLContext context(Credential credential, List<X509Certificate> trusted) throws GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(credential == null ? null : new KeyManager[] {new Fixed(credential)}, new TrustManager[] {trustManager(trusted)}, null);
        return context;
    }
}
