package com.jk.explore.security.localca;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
 * Certificate signing requests (PKCS#10). A service signs its request with its own private key, so the authority can check that whoever
 * sent it holds that key ("proof of possession") without ever seeing the key.
 */
final class Csr {

    private Csr() {
    }

    /** A request for the given names, signed by the same key it carries. */
    static PKCS10CertificationRequest create(KeyPair keys, List<String> names) {
        return build(keys.getPublic(), keys.getPrivate(), names);
    }

    /** A request that carries one public key but is signed by another: someone asking for a certificate on a key they do not hold. */
    static PKCS10CertificationRequest signedByAnotherKey(PublicKey claimed, PrivateKey signer, List<String> names) {
        return build(claimed, signer, names);
    }

    private static PKCS10CertificationRequest build(PublicKey key, PrivateKey signer, List<String> names) {
        try {
            JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Name("CN=" + (names.isEmpty() ? "unnamed" : names.get(0))), key);
            if (!names.isEmpty()) {
                GeneralName[] general = names.stream().map(n -> new GeneralName(GeneralName.dNSName, n)).toArray(GeneralName[]::new);
                ExtensionsGenerator extensions = new ExtensionsGenerator();
                extensions.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(general));
                builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
            }
            return builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signer));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** True when the request's signature verifies with the public key inside it. */
    static boolean provesPossession(PKCS10CertificationRequest request) {
        try {
            return request.isSignatureValid(new JcaContentVerifierProviderBuilder().build(request.getSubjectPublicKeyInfo()));
        } catch (Exception e) {
            return false;
        }
    }

    /** The DNS names the request asks for, from its extension request attribute. */
    static List<String> names(PKCS10CertificationRequest request) {
        List<String> names = new ArrayList<>();
        for (Attribute attribute : request.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
            Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
            GeneralNames general = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
            if (general != null) {
                for (GeneralName name : general.getNames()) {
                    if (name.getTagNo() == GeneralName.dNSName) {
                        names.add(String.valueOf(name.getName()));
                    }
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
}
