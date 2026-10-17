package com.jk.explore.security.certsbc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * How a shop gets a certificate without ever handing over its private key: it signs a request (PKCS#10) with that key, naming itself and
 * carrying its public key. The authority checks the signature, which proves the shop holds the private key, applies its own policy to the
 * names, and signs a certificate for that public key.
 */
final class SigningRequest {

    /** The authority refused to certify the request. */
    static final class RefusedException extends Exception {

        RefusedException(String message) {
            super(message);
        }
    }

    private SigningRequest() {
    }

    static PKCS10CertificationRequest create(KeyPair keys, String host) {
        try {
            ExtensionsGenerator extensions = new ExtensionsGenerator();
            extensions.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, host)));
            JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + host), keys.getPublic());
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
            return builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** True if the request's signature verifies with the public key inside it: whoever sent it holds the matching private key. */
    static boolean provesPossession(PKCS10CertificationRequest request) {
        try {
            return request.isSignatureValid(new JcaContentVerifierProviderBuilder().build(request.getSubjectPublicKeyInfo()));
        } catch (Exception e) {
            return false;
        }
    }

    static List<String> requestedNames(PKCS10CertificationRequest request) {
        List<String> names = new ArrayList<>();
        for (Attribute attribute : request.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
            Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
            GeneralNames general = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
            if (general != null) {
                for (GeneralName name : general.getNames()) {
                    names.add(String.valueOf(name.getName()));
                }
            }
        }
        return names;
    }

    static PublicKey publicKey(PKCS10CertificationRequest request) {
        try {
            return new JcaPKCS10CertificationRequest(request).getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static PKCS10CertificationRequest parse(byte[] der) throws IOException {
        return new PKCS10CertificationRequest(der);
    }

    /** One letter of the host name inside the signed request is changed, as a bit error or an attacker would. */
    static byte[] changeName(PKCS10CertificationRequest request, String host) throws IOException {
        byte[] bytes = request.getEncoded();
        byte[] needle = host.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i + needle.length <= bytes.length; i++) {
            int j = 0;
            while (j < needle.length && bytes[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) {
                bytes[i + needle.length - 1] -= 32;
                return bytes;
            }
        }
        throw new IllegalArgumentException("the name is not in the request");
    }

    /**
     * The authority's side: check the signature, then certify only names under the one domain it serves, for one year from the given day.
     * The certificate carries the public key from the request: the authority never sees the private key.
     */
    static X509Certificate certify(PKCS10CertificationRequest request, Issued authority, String domain, long serial, LocalDate from) throws RefusedException {
        if (!provesPossession(request)) {
            throw new RefusedException("the request's signature does not match its key");
        }
        List<String> names = requestedNames(request);
        if (names.isEmpty()) {
            throw new RefusedException("the request names nothing");
        }
        for (String name : names) {
            if (!name.equals(domain) && !name.endsWith("." + domain)) {
                throw new RefusedException(name + " is outside " + domain);
            }
        }
        String host = names.get(0);
        return CertFactory.issueFor(CertFactory.Spec.server(host, from, from.plusYears(1), serial), publicKey(request), authority);
    }
}
