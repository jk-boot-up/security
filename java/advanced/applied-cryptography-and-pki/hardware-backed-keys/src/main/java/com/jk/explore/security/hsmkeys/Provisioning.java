package com.jk.explore.security.hsmkeys;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;

/** Key ceremony: the bank's payment-signing key is born on the token with its private half sensitive and not extractable. */
final class Provisioning {

    static final String ORG = "Demo Bank";
    static final String CN = "Payments Signing";

    private Provisioning() {
    }

    /** What the ceremony produced: the public key (which is all that ever leaves the token) and the certificate. */
    record Provisioned(PublicKey publicKey, byte[] certificate, long privateHandle) {
    }

    static Provisioned rsaSigningKey(Raw raw, String tokenLabel, String pin, String alias) throws Exception {
        long slot = raw.slotWithLabel(tokenLabel);
        long s = raw.openSession(slot);
        try {
            Raw.Answer login = raw.login(s, pin);
            if (!login.ok() && login.rv() != 0x100) {
                throw new IllegalStateException("login " + login.name());
            }
            byte[] id = {0x01};
            long[] pair = raw.generateRsaPair(s, alias, id);
            BigInteger n = new BigInteger(1, raw.attribute(s, pair[0], Raw.CKA_MODULUS).bytes());
            BigInteger e = new BigInteger(1, raw.attribute(s, pair[0], Raw.CKA_PUBLIC_EXPONENT).bytes());
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
            byte[] der = Der.selfSignedRsa(ORG, CN, pub, BigInteger.valueOf(1001), "260101000000Z", "360101000000Z", tbs -> {
                Raw.Answer sig = raw.sign(s, Raw.CKM_SHA256_RSA_PKCS, pair[1], tbs);
                if (!sig.ok()) {
                    throw new IllegalStateException("sign " + sig.name());
                }
                return sig.bytes();
            });
            raw.createCertificate(s, der, Der.name(ORG, CN), id, alias);
            return new Provisioned(pub, der, pair[1]);
        } finally {
            raw.logout(s);
            raw.closeSession(s);
        }
    }

    /** Finds the private key objects of a token, as a logged-in session sees them. */
    static List<Long> privateKeys(Raw raw, long session) {
        return raw.find(session, List.of(Raw.Attr.ulong(Raw.CKA_CLASS, Raw.CKO_PRIVATE_KEY), Raw.Attr.ulong(Raw.CKA_KEY_TYPE, Raw.CKK_RSA)));
    }
}
