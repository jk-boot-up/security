package com.jk.explore.security.ca;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small certificate authority: one root key, a serial number counter, a list of revoked serials, and a revocation list it can publish.
 * Keys never leave this object; what it hands out is certificates and lists.
 */
public final class LocalCa {

    private final String name;
    private final KeyPair key;
    private final X509Certificate root;
    private final Map<BigInteger, Instant> revoked = new LinkedHashMap<>();
    private long nextSerial = 1000;
    private long crlNumber;

    public LocalCa(String name, Instant now) {
        this.name = name;
        this.key = newKey();
        this.root = Certificates.build(new Certificates.Spec(name, name, BigInteger.ONE, now.minus(Duration.ofMinutes(1)), now.plus(Duration.ofDays(3650)), true, List.of()),
                key.getPublic(), key.getPrivate());
    }

    public static KeyPair newKey() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            return g.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public X509Certificate rootCertificate() {
        return root;
    }

    /** Issues a server certificate for {@code dnsNames}, good for {@code lifetime} from {@code now}. */
    public X509Certificate issue(String commonName, List<String> dnsNames, PublicKey subjectKey, Instant now, Duration lifetime) {
        return Certificates.build(new Certificates.Spec(commonName, name, BigInteger.valueOf(nextSerial++), now.minus(Duration.ofMinutes(1)), now.plus(lifetime), false, dnsNames),
                subjectKey, key.getPrivate());
    }

    /** Renewal is a new certificate for the same key and names, with a new serial number and a new validity period. */
    public X509Certificate renew(X509Certificate old, List<String> dnsNames, Instant now, Duration lifetime) {
        return issue(old.getSubjectX500Principal().getName().replace("CN=", ""), dnsNames, old.getPublicKey(), now, lifetime);
    }

    public void revoke(X509Certificate cert, Instant now) {
        revoked.put(cert.getSerialNumber(), now);
    }

    /** Signs and publishes the current revocation list, valid for {@code validFor}. */
    public X509CRL publishCrl(Instant now, Duration validFor) {
        return Certificates.crl(name, key.getPrivate(), now, now.plus(validFor), ++crlNumber, revoked);
    }

    /** For the attack demonstrations only: a certificate signed by somebody else's key, claiming this CA's name. */
    X509Certificate forge(String commonName, List<String> dnsNames, PublicKey subjectKey, KeyPair signer, Instant now) {
        return Certificates.build(new Certificates.Spec(commonName, name, BigInteger.valueOf(666), now.minus(Duration.ofMinutes(1)), now.plus(Duration.ofDays(30)), false, dnsNames),
                subjectKey, signer.getPrivate());
    }
}
