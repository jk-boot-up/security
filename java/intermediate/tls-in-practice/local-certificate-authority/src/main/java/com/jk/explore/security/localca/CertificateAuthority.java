package com.jk.explore.security.localca;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/**
 * The store's internal certificate authority. An offline root signs one issuing authority and a revocation list, and is then put away.
 * The issuing authority is limited by name constraints to {@code shop.internal}, may not certify further authorities (path length zero),
 * and issues end-entity certificates only through {@link #issue}, which applies the requester's allow-list, proof of possession and a
 * profile. Every decision goes to an audit log.
 */
final class CertificateAuthority {

    static final String DOMAIN = "shop.internal";

    private final Issued root;
    private final Issued issuing;
    private final X500Name issuingName = new X500Name("CN=Shop Issuing CA,O=Example Shop");
    private final Map<String, Set<String>> allowedNames;
    private final Map<BigInteger, Integer> revoked = new LinkedHashMap<>();
    private final List<String> audit = new ArrayList<>();
    private long nextSerial = 1000;
    private int rootSignatures;

    CertificateAuthority(Instant start, Map<String, Set<String>> allowedNames) {
        this.allowedNames = Map.copyOf(allowedNames);
        var rootKeys = Keys.newPair();
        X500Name rootName = new X500Name("CN=Shop Internal Root CA,O=Example Shop");
        this.root = new Issued(rootKeys, build(rootName, rootName, BigInteger.ONE, start, start.plus(Duration.ofDays(20 * 365)),
                rootKeys.getPublic(), rootKeys.getPrivate(), null, Kind.ROOT, List.of(), null));
        rootSignatures++;
        var issuingKeys = Keys.newPair();
        this.issuing = new Issued(issuingKeys, build(issuingName, rootName, BigInteger.TWO, start,
                start.plus(Duration.ofDays(5 * 365)), issuingKeys.getPublic(), rootKeys.getPrivate(), root.certificate(), Kind.ISSUING, List.of(), null));
        rootSignatures++;
        audit.add("root signed the issuing authority, serial 2, path length 0, names limited to " + DOMAIN);
    }

    private enum Kind { ROOT, ISSUING, END_ENTITY }

    X509Certificate root() {
        return root.certificate();
    }

    X509Certificate issuing() {
        return issuing.certificate();
    }

    /** How many times the root's private key has been used. An offline root is used rarely, on purpose. */
    int rootSignatures() {
        return rootSignatures;
    }

    List<String> audit() {
        return List.copyOf(audit);
    }

    /**
     * The only way an end-entity certificate is made. The checks run in this order: proof of possession, at least one name, every name
     * inside the domain and on the requester's allow-list, and the lifetime within the profile.
     */
    X509Certificate issue(PKCS10CertificationRequest request, String requester, Profile profile, Duration lifetime, Instant now) throws RefusedException {
        try {
            if (!Csr.provesPossession(request)) {
                throw new RefusedException("the request is not signed by the key it carries");
            }
            List<String> names = Csr.names(request);
            if (names.isEmpty()) {
                throw new RefusedException("a certificate needs at least one subject alternative name");
            }
            Set<String> allowed = allowedNames.getOrDefault(requester, Set.of());
            for (String name : names) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (!inDomain(lower)) {
                    throw new RefusedException(name + " is outside " + DOMAIN);
                }
                if (!allowed.contains(lower)) {
                    throw new RefusedException(requester + " may not ask for " + name);
                }
            }
            if (lifetime.compareTo(profile.maxLifetime()) > 0) {
                throw new RefusedException("asked for " + describe(lifetime) + ", the " + profile.name() + " profile allows at most " + describe(profile.maxLifetime()));
            }
            X509Certificate certificate = endEntity(Csr.publicKey(request), names, profile.purpose(), now, now.plus(lifetime));
            audit.add("issued serial " + certificate.getSerialNumber() + " to " + requester + " for " + String.join(", ", names)
                    + ", " + describe(lifetime) + ", profile " + profile.name());
            return certificate;
        } catch (RefusedException e) {
            audit.add("refused " + requester + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * What a careless or compromised issuer could sign: a certificate with no policy applied. The name constraint in the issuing
     * authority's own certificate is then the last line, enforced by every client that validates the chain.
     */
    X509Certificate issueWithoutPolicy(PublicKey key, String name, Instant now) {
        X509Certificate certificate = endEntity(key, List.of(name), KeyPurposeId.id_kp_serverAuth, now, now.plus(Duration.ofDays(90)));
        audit.add("issued serial " + certificate.getSerialNumber() + " for " + name + " WITHOUT THE POLICY");
        return certificate;
    }

    private X509Certificate endEntity(PublicKey key, List<String> names, KeyPurposeId purpose, Instant from, Instant to) {
        BigInteger serial = BigInteger.valueOf(nextSerial++);
        return build(new X500Name("CN=" + names.get(0)), issuingName, serial, from, to,
                key, issuing.keys().getPrivate(), issuing.certificate(), Kind.END_ENTITY, names, purpose);
    }

    private static boolean inDomain(String name) {
        return name.endsWith("." + DOMAIN) && name.length() > DOMAIN.length() + 1;
    }

    static String describe(Duration duration) {
        long hours = duration.toHours();
        return hours % 24 == 0 ? hours / 24 + " days" : hours + " hours";
    }

    void revoke(BigInteger serial, int reason, Instant when) {
        revoked.put(serial, reason);
        audit.add("revoked serial " + serial + " (" + reasonName(reason) + ")");
    }

    static String reasonName(int reason) {
        return switch (reason) {
            case CRLReason.keyCompromise -> "key compromise";
            case CRLReason.cACompromise -> "CA compromise";
            case CRLReason.superseded -> "superseded";
            default -> "reason " + reason;
        };
    }

    /** The issuing authority's revocation list, valid from {@code thisUpdate} until {@code thisUpdate + validFor}. */
    X509CRL issuingCrl(Instant thisUpdate, Duration validFor) {
        return crl(issuing, thisUpdate, validFor, revoked);
    }

    /** The root's revocation list, covering the issuing authority. Each one is a use of the offline root key. */
    X509CRL rootCrl(Instant thisUpdate, Duration validFor, boolean issuingRevoked) {
        rootSignatures++;
        return crl(root, thisUpdate, validFor, issuingRevoked ? Map.of(BigInteger.TWO, CRLReason.cACompromise) : Map.of());
    }

    private static X509CRL crl(Issued signer, Instant thisUpdate, Duration validFor, Map<BigInteger, Integer> entries) {
        try {
            X509v2CRLBuilder builder = new X509v2CRLBuilder(new JcaX509CertificateHolder(signer.certificate()).getSubject(), Date.from(thisUpdate));
            builder.setNextUpdate(Date.from(thisUpdate.plus(validFor)));
            for (Map.Entry<BigInteger, Integer> entry : entries.entrySet()) {
                builder.addCRLEntry(entry.getKey(), Date.from(thisUpdate), entry.getValue());
            }
            builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(signer.certificate()));
            return new JcaX509CRLConverter().getCRL(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signer.keys().getPrivate())));
        } catch (Exception e) {
            throw new IllegalStateException("could not build the revocation list", e);
        }
    }

    private static X509Certificate build(X500Name subject, X500Name issuerName, BigInteger serial, Instant from, Instant to, PublicKey subjectKey,
            PrivateKey signingKey, X509Certificate issuerCertificate, Kind kind, List<String> names, KeyPurposeId purpose) {
        try {
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, Date.from(from), Date.from(to), subject, subjectKey);
            JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(subjectKey));
            builder.addExtension(Extension.authorityKeyIdentifier, false,
                    issuerCertificate == null ? utils.createAuthorityKeyIdentifier(subjectKey) : utils.createAuthorityKeyIdentifier(issuerCertificate));
            switch (kind) {
                case ROOT -> {
                    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(1));
                    builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
                }
                case ISSUING -> {
                    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
                    builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
                    GeneralSubtree permitted = new GeneralSubtree(new GeneralName(GeneralName.dNSName, DOMAIN));
                    builder.addExtension(Extension.nameConstraints, true, new NameConstraints(new GeneralSubtree[] {permitted}, null));
                }
                case END_ENTITY -> {
                    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
                    builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
                    builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purpose));
                    GeneralName[] general = names.stream().map(n -> new GeneralName(GeneralName.dNSName, n)).toArray(GeneralName[]::new);
                    builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(general));
                }
            }
            return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signingKey)));
        } catch (Exception e) {
            throw new IllegalStateException("could not build the certificate " + subject, e);
        }
    }
}
