package com.jk.explore.security.signaturesbc;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;

/** Runs the online bank story in five numbered acts, with Bouncy Castle, and prints facts that do not depend on random keys. */
public class SignaturesBcDemo {

    static final byte[] STATEMENT = Hex.bytes("Statement ANA-1: balance 1200");
    static final Instant SIGNING_TIME = Instant.parse("2026-03-01T09:30:00Z");

    // RFC 8032 section 7.1, test 1 (Ed25519, empty message) and section 7.4 (Ed448, empty message).
    static final String ED25519_SEED = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";
    static final String ED25519_SIGNATURE = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b";
    static final String ED448_SEED = "6c82a562cb808d10d632be89c8513ebf6c929f34ddfa8c9f63c9960ef6e348a3528c8a3fcc2f044e39a3fc5b94492f8f032e7549a20098f95b";
    static final String ED448_SIGNATURE = "533a37f6bbe457251f023c0d88f976ae2dfb504a843e34d2074fd823d41a591f2b233f034f628281f2fd7a22ddd47d7828c59bd0a21bfd3980ff0d2028d4b18a9df63e006c5d1c2d345b925d8dc00b4104852db99ac5c7cdda8530a113a0f4dbb61149f05a7363268c71d95808ff2e652600";

    // RFC 6979 appendix A.2.5, P-256 with SHA-256, message "sample".
    static final String P256_KEY = "C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721";
    static final String P256_R = "EFD48B2AACB6A8FD1140DD9CD45E81D69D2C877B56AAF991C34D0EA84EAF3716";
    static final String P256_S = "F7CB1C942D657C41D436C7A1B6E29F65F3E900DBB9AFF4064DC4AB2F843ACDA8";

    // RFC 8037 appendix A.4: an Ed25519 JWS.
    static final String RFC8037_SEED = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";
    static final String RFC8037_PAYLOAD = "Example of Ed25519 signing";
    static final String RFC8037_TOKEN = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc.hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Cms(out);
        act2Ecdsa(out);
        act3EdDsa(out);
        act4Jws(out);
        act5PemAndBill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Cms(StringBuilder out) {
        line(out, "ONE. A standard signed message: CMS SignedData.");
        KeyPair bank = Certificates.newEcKeyPair();
        X509Certificate cert = Certificates.selfSigned("Statements", bank);
        X509Certificate other = Certificates.selfSigned("Somebody else", Certificates.newEcKeyPair());
        byte[] attached = Cms.sign(cert, bank.getPrivate(), STATEMENT, true, SIGNING_TIME);
        byte[] detached = Cms.sign(cert, bank.getPrivate(), STATEMENT, false, SIGNING_TIME);
        line(out, "  the bank signs a statement with its ECDSA key and puts its certificate in the message");
        line(out, "  attached (the statement travels inside) verifies with the certificate: " + Cms.verify(attached, null, cert));
        line(out, "  detached (the statement travels separately) verifies when the statement is supplied: " + Cms.verify(detached, STATEMENT, cert));
        byte[] changed = Hex.bytes("Statement ANA-1: balance 9200");
        line(out, "  the detached signature with a changed statement: " + verdict(Cms.verify(detached, changed, cert)));
        byte[] tampered = attached.clone();
        int at = indexOf(tampered, STATEMENT);
        tampered[at + STATEMENT.length - 1] ^= 1;
        line(out, "  the attached message with one bit changed inside it: " + verdict(Cms.verify(tampered, null, cert)));
        line(out, "  checked against somebody else's certificate: " + verdict(Cms.verify(attached, null, other)));
        line(out, "  the signing time inside is " + Cms.claimedSigningTime(attached) + ": covered by the signature, but it is the signer's word");
    }

    private static void act2Ecdsa(StringBuilder out) {
        line(out, "TWO. ECDSA: the nonce that must never repeat.");
        BigInteger d = new BigInteger(P256_KEY, 16);
        var key = Ecdsa.publicFor(d);
        byte[] sample = Hex.bytes("sample");
        Ecdsa.Signature first = Ecdsa.signDeterministic(d, sample);
        Ecdsa.Signature second = Ecdsa.signDeterministic(d, sample);
        boolean rfc = first.r().equals(new BigInteger(P256_R, 16)) && first.s().equals(new BigInteger(P256_S, 16));
        line(out, "  deterministic signing (RFC 6979) of the same message twice gives the same signature: " + first.equals(second));
        line(out, "  and it matches the value published in RFC 6979 appendix A.2.5: " + rfc);
        Ecdsa.Signature random1 = Ecdsa.signRandomized(d, sample);
        Ecdsa.Signature random2 = Ecdsa.signRandomized(d, sample);
        line(out, "  randomized signing of the same message twice gives different signatures: " + !random1.equals(random2)
                + ", and both verify: " + (Ecdsa.verify(key, sample, random1) && Ecdsa.verify(key, sample, random2)));
        BigInteger k = new BigInteger("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", 16);
        byte[] m1 = Hex.bytes("pay 10 to Bob");
        byte[] m2 = Hex.bytes("pay 99 to Eve");
        Ecdsa.Signature s1 = Ecdsa.signWithNonce(d, m1, k);
        Ecdsa.Signature s2 = Ecdsa.signWithNonce(d, m2, k);
        boolean recovered = Ecdsa.recoverKeyFromReusedNonce(m1, s1, m2, s2).equals(d);
        line(out, "  two signatures made with the same nonce let anyone compute the private key: " + recovered + "  <- NEVER REUSE A NONCE");
        line(out, "  deterministic signing removes the risk, and so does Ed25519, which is the next demo");
    }

    private static void act3EdDsa(StringBuilder out) {
        line(out, "THREE. EdDSA: Ed25519 and Ed448.");
        byte[] seed25519 = Hex.parse(ED25519_SEED);
        byte[] pub25519 = EdDsa.public25519(seed25519);
        byte[] empty = new byte[0];
        byte[] sig25519 = EdDsa.sign25519(seed25519, empty);
        line(out, "  an Ed25519 signature is " + sig25519.length + " bytes and matches RFC 8032: " + Hex.of(sig25519).equals(ED25519_SIGNATURE));
        byte[] seed448 = Hex.parse(ED448_SEED);
        byte[] pub448 = EdDsa.public448(seed448);
        byte[] sig448 = EdDsa.sign448(seed448, empty);
        line(out, "  an Ed448 signature is " + sig448.length + " bytes and matches RFC 8032: " + Hex.of(sig448).equals(ED448_SIGNATURE));
        line(out, "  signing again gives the same bytes, with no random number involved: "
                + Arrays.equals(sig25519, EdDsa.sign25519(seed25519, empty)) + " and " + Arrays.equals(sig448, EdDsa.sign448(seed448, empty)));
        byte[] flipped = sig25519.clone();
        flipped[10] ^= 1;
        line(out, "  both verify: " + (EdDsa.verify25519(pub25519, empty, sig25519) && EdDsa.verify448(pub448, empty, sig448))
                + "; one flipped bit in an Ed25519 signature: " + verdict(EdDsa.verify25519(pub25519, empty, flipped)));
    }

    private static void act4Jws(StringBuilder out) {
        line(out, "FOUR. A compact signed token: JWS.");
        byte[] seed = Hex.parse(RFC8037_SEED);
        byte[] pub = EdDsa.public25519(seed);
        byte[] payload = Hex.bytes(RFC8037_PAYLOAD);
        String token = Jws.sign(seed, payload);
        line(out, "  signing the RFC 8037 example gives exactly the token in the RFC: " + token.equals(RFC8037_TOKEN));
        line(out, "  it verifies: " + Jws.verify(pub, token, null));
        String detached = Jws.signDetached(seed, payload);
        line(out, "  detached form has an empty middle part (" + detached.split("\\.", -1)[1].isEmpty() + ") and verifies when the payload is supplied: " + Jws.verify(pub, detached, payload));
        String[] parts = token.split("\\.");
        String none = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(Hex.bytes("{\"alg\":\"none\"}")) + "." + parts[1] + ".";
        line(out, "  a token that claims alg none and has no signature: " + verdict(Jws.verify(pub, none, null)));
        String changed = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(Hex.bytes("Example of Ed25519 forging")) + "." + parts[2];
        line(out, "  the same signature with a different payload: " + verdict(Jws.verify(pub, changed, null)));
        line(out, "  the verifier decides the algorithm, not the token: that is what stops alg none and key confusion");
    }

    private static void act5PemAndBill(StringBuilder out) {
        line(out, "FIVE. Text armor, and the bill.");
        KeyPair bank = Certificates.newEcKeyPair();
        X509Certificate cert = Certificates.selfSigned("Statements", bank);
        byte[] signed = Cms.sign(cert, bank.getPrivate(), STATEMENT, false, SIGNING_TIME);
        String pem = PemSignatures.write("CMS", signed);
        line(out, "  a signed message as PEM text begins with -----BEGIN CMS-----: " + pem.startsWith("-----BEGIN CMS-----"));
        line(out, "  and reads back to the same bytes: " + Arrays.equals(signed, PemSignatures.read("CMS", pem)));
        line(out, "  a valid signature proves that the key signed, not who owns the key: that needs a chain of certificates");
        line(out, "  the signing time is the signer's claim: a trusted time needs a timestamp authority (RFC 3161), described here and not run");
        line(out, "  the private key still needs a home: see keystore-basics");
        line(out, "  openssl checks these same files in the integration tests: cms -verify, pkeyutl -verify and dgst -verify");
    }

    private static String verdict(boolean accepted) {
        return accepted ? "accepted  <- WRONG" : "refused";
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new IllegalStateException("pattern not found");
    }
}
