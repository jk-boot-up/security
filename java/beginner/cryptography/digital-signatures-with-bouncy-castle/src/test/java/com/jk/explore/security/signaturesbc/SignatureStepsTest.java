package com.jk.explore.security.signaturesbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** The by-hand steps must give exactly the library's signature and the published RFC 6979 values. */
class SignatureStepsTest {

    private static final BigInteger KEY = new BigInteger(SignaturesBcDemo.P256_KEY, 16);

    @Test
    void stepsGiveBouncyCastlesDeterministicSignature() {
        SignatureSteps.Steps steps = SignatureSteps.sign(KEY, SignaturesBcDemo.STATEMENT);
        assertEquals(Ecdsa.signDeterministic(KEY, SignaturesBcDemo.STATEMENT), new Ecdsa.Signature(steps.r(), steps.s()));
    }

    @Test
    void stepsMatchRfc6979() {
        SignatureSteps.Steps steps = SignatureSteps.sign(KEY, Hex.bytes("sample"));
        assertEquals(new BigInteger(SignaturesBcDemo.P256_R, 16), steps.r());
        assertEquals(new BigInteger(SignaturesBcDemo.P256_S, 16), steps.s());
        // the k published in RFC 6979 appendix A.2.5 for SHA-256 and "sample"
        assertEquals(new BigInteger("A6E3C57DD01ABE90086538398355DD4C3B17AA873382B0F24D6129493D8AAD60", 16), steps.k());
    }

    @Test
    void theCheckGivesBackROnlyForTheSignedMessage() {
        SignatureSteps.Steps steps = SignatureSteps.sign(KEY, SignaturesBcDemo.STATEMENT);
        var q = Ecdsa.publicFor(KEY).getQ();
        assertEquals(steps.r(), SignatureSteps.check(q, SignaturesBcDemo.STATEMENT, steps.r(), steps.s()));
        assertNotEquals(steps.r(), SignatureSteps.check(q, SignaturesBcDemo.CHANGED, steps.r(), steps.s()));
    }

    @Test
    void theCmsDigestAttributeIsTheSha256OfTheContent() {
        var bank = Certificates.newEcKeyPair();
        var cert = Certificates.selfSigned("Statements", bank);
        byte[] signed = Cms.sign(cert, bank.getPrivate(), SignaturesBcDemo.STATEMENT, false, SignaturesBcDemo.SIGNING_TIME);
        assertArrayEquals(Ecdsa.sha256(SignaturesBcDemo.STATEMENT), Cms.signedDigest(signed));
    }
}
