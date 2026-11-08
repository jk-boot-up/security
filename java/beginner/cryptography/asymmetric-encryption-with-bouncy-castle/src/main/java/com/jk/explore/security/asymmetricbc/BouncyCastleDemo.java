package com.jk.explore.security.asymmetricbc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Runs the online bank story in six numbered acts, with Bouncy Castle and OpenSSL, and prints facts that do not depend on random keys. */
public class BouncyCastleDemo {

    static final byte[] ACCOUNT = Hex.bytes("account=ANA-1");
    static final char[] PASSWORD = "correct horse battery staple".toCharArray();

    static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A 10,000-byte statement: 625 lines of 16 bytes, all the same rent payment. */
    static byte[] statement() {
        return Hex.bytes("RENT      -1200\n".repeat(625));
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text. Act five needs the openssl command-line tool, so its test is tagged "openssl". */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        KeyPair ana = newKeyPair();
        act1Pem(out, ana);
        act2PrivateKey(out, ana);
        X509Certificate cert = act3Certificate(out, ana);
        byte[] envelope = act4Envelope(out, ana, cert);
        act5ByHand(out, ana, cert, envelope);
        act6Bill(out, cert);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }

    private static String hex(byte[] bytes, int from, int count) {
        StringBuilder s = new StringBuilder();
        for (int i = from; i < from + count; i++) {
            s.append(i > from && (i - from) % 4 == 0 ? " " : "").append(String.format("%02x", bytes[i]));
        }
        return s.toString();
    }

    private static void act1Pem(StringBuilder out, KeyPair ana) {
        line(out, "ONE. The public key as PEM text.");
        byte[] der = ana.getPublic().getEncoded();
        String pem = Pem.publicKey(ana.getPublic());
        List<String> pemLines = pem.lines().toList();
        String body = String.join("", pemLines.subList(1, pemLines.size() - 1));
        line(out, "  BEFORE PEM (the key as bytes):");
        line(out, "    size         " + der.length + " bytes");
        line(out, "    first bytes  " + hex(der, 0, 16));
        line(out, "  AFTER PEM (text between a BEGIN and an END line):");
        line(out, "    first line   " + pemLines.get(0));
        line(out, "    text begins  " + body.substring(0, 32) + "...");
        line(out, "    size         " + body.length() + " letters, in " + (pemLines.size() - 2) + " lines of up to 64");
        line(out, "    last line    " + pemLines.get(pemLines.size() - 1));
        line(out, "  it reads back to the same key: " + Pem.readPublicKey(pem).equals(ana.getPublic()));
        line(out, "  BASE64, STEP BY STEP (the first 3 bytes):");
        int[] g = Base64Steps.groups(der[0], der[1], der[2]);
        line(out, "    bytes        " + hex(der, 0, 1) + "       " + hex(der, 1, 1) + "       " + hex(der, 2, 1));
        line(out, "    bits         " + Base64Steps.bits(der[0] & 0xff, 8) + " " + Base64Steps.bits(der[1] & 0xff, 8)
                + " " + Base64Steps.bits(der[2] & 0xff, 8));
        StringBuilder six = new StringBuilder();
        StringBuilder numbers = new StringBuilder();
        StringBuilder letters = new StringBuilder();
        for (int n : g) {
            six.append(Base64Steps.bits(n, 6)).append("   ");
            numbers.append(String.format("%-9d", n));
            letters.append(String.format("%-9s", Base64Steps.ALPHABET.charAt(n)));
        }
        line(out, "    6-bit groups " + six.toString().strip());
        line(out, "    numbers      " + numbers.toString().strip());
        line(out, "    letters      " + letters.toString().strip());
        line(out, "  all " + der.length + " bytes by hand = the PEM text: " + Base64Steps.encode(der).equals(body));
    }

    private static void act2PrivateKey(StringBuilder out, KeyPair ana) {
        line(out, "TWO. The private key file, locked with a password.");
        String clear = Pem.privateKeyInTheClear(ana.getPrivate());
        String locked = Pem.privateKeyEncrypted(ana.getPrivate(), PASSWORD);
        line(out, "  BEFORE LOCKING (the key in the clear):");
        line(out, "    first line   " + firstLine(clear));
        line(out, "    no password needed: " + Pem.readPrivateKey(clear).equals(ana.getPrivate()) + "  <- ANYONE CAN USE IT");
        line(out, "  AFTER LOCKING (AES-256, a key made from the password):");
        line(out, "    first line   " + firstLine(locked));
        line(out, "    right password: the same key: " + Pem.readEncryptedPrivateKey(locked, PASSWORD).equals(ana.getPrivate()));
        line(out, "    wrong password: " + wrongPassword(locked));
    }

    private static X509Certificate act3Certificate(StringBuilder out, KeyPair ana) {
        line(out, "THREE. A certificate for the public key.");
        X509Certificate cert = Certificates.selfSigned("Ana", ana);
        line(out, "  BEFORE SIGNING (the inputs):");
        line(out, "    name         " + cert.getSubjectX500Principal().getName());
        line(out, "    public key   Ana's, " + ana.getPublic().getEncoded().length + " bytes");
        line(out, "    valid        " + cert.getNotBefore().toInstant() + " to " + cert.getNotAfter().toInstant());
        line(out, "  AFTER SIGNING (the certificate, " + encodedLength(cert) + " bytes):");
        line(out, "    subject      " + cert.getSubjectX500Principal().getName());
        line(out, "    issuer       " + cert.getIssuerX500Principal().getName());
        line(out, "    serial       " + cert.getSerialNumber());
        line(out, "    signed with  " + cert.getSigAlgName() + ", by Ana's own private key");
        line(out, "    as text      " + firstLine(Pem.certificate(cert)));
        line(out, "  issuer equals subject: " + cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())
                + "  <- ANYONE CAN MAKE ONE");
        return cert;
    }

    private static byte[] act4Envelope(StringBuilder out, KeyPair ana, X509Certificate cert) {
        line(out, "FOUR. A standard envelope: CMS EnvelopedData.");
        byte[] statement = statement();
        byte[] envelope = CmsEnvelope.seal(cert, statement);
        EnvelopeSteps.Parts parts = EnvelopeSteps.parts(envelope);
        line(out, "  BEFORE SEALING:");
        line(out, "    statement    " + statement.length + " bytes, 625 lines of " + firstLine(Hex.text(statement)));
        line(out, "    recipient    Ana's certificate, serial " + cert.getSerialNumber());
        line(out, "  AFTER SEALING (one envelope, " + envelope.length + " bytes):");
        line(out, "    for          the certificate with serial " + parts.serial());
        line(out, "    locked key   " + parts.lockedKey().length + " bytes: a new AES key, RSA-OAEP");
        line(out, "    IV           " + parts.iv().length + " bytes, for AES-256-CBC");
        line(out, "    cipher text  " + parts.cipherText().length + " bytes: the statement");
        line(out, "  OPENING IT:");
        line(out, "    Ana, her private key: " + Arrays.equals(CmsEnvelope.open(ana.getPrivate(), cert, envelope), statement));
        KeyPair eve = newKeyPair();
        line(out, "    Eve, her own private key: " + eveResult(eve, cert, envelope) + "  <- NOT FOR HER");
        return envelope;
    }

    private static void act5ByHand(StringBuilder out, KeyPair ana, X509Certificate cert, byte[] envelope)
            throws IOException, InterruptedException {
        line(out, "FIVE. Opening it by hand, then with openssl.");
        EnvelopeSteps.Parts parts = EnvelopeSteps.parts(envelope);
        EnvelopeSteps.Opened opened = EnvelopeSteps.open(ana.getPrivate(), parts);
        byte[] padded = opened.padded();
        line(out, "  STEP 1. UNLOCK THE AES KEY (RSA-OAEP, Ana's private key):");
        line(out, "    locked key   " + parts.lockedKey().length + " bytes in");
        line(out, "    AES key      " + opened.dataKey().length + " bytes out: 256 bits");
        line(out, "  STEP 2. DECRYPT (AES-256-CBC, that key and the IV):");
        line(out, "    cipher text  " + parts.cipherText().length + " bytes in");
        line(out, "    plain text   " + padded.length + " bytes out");
        line(out, "    last block   " + hex(padded, padded.length - 16, 16));
        line(out, "  STEP 3. REMOVE THE PADDING (the last byte says how many):");
        line(out, "    0x" + hex(padded, padded.length - 1, 1) + " = " + padded[padded.length - 1] + " bytes off: "
                + opened.statement().length + " bytes, the statement");
        line(out, "  the JDK alone opened it, as Bouncy Castle did: " + Arrays.equals(opened.statement(), statement()));
        line(out, "  OPENSSL, A DIFFERENT PROGRAM (envelope, key file, certificate):");
        if (!OpenSsl.available()) {
            line(out, "    openssl is not installed, so this check was skipped");
            return;
        }
        OpenSsl.Result result = OpenSsl.decrypt(envelope, Pem.privateKeyInTheClear(ana.getPrivate()), Pem.certificate(cert));
        line(out, "    openssl cms -decrypt opens it: " + Arrays.equals(result.output(), statement())
                + "  <- A REAL STANDARD");
    }

    private static void act6Bill(StringBuilder out, X509Certificate cert) throws IOException {
        line(out, "SIX. The bill: what Bouncy Castle costs.");
        byte[] oaep = CmsEnvelope.seal(cert, Hex.bytes("x"), true);
        byte[] easy = CmsEnvelope.seal(cert, Hex.bytes("x"), false);
        line(out, "  THE CAREFUL CALL (OAEP asked for by name):");
        String oaepOid = CmsEnvelope.keyTransportAlgorithm(oaep);
        String easyOid = CmsEnvelope.keyTransportAlgorithm(easy);
        line(out, "    key locked with  " + algorithmName(oaepOid));
        line(out, "    OID              " + oaepOid);
        line(out, "  THE EASY CALL (the one most examples show):");
        line(out, "    key locked with  " + algorithmName(easyOid) + "  <- OLDER PADDING");
        line(out, "    OID              " + easyOid);
        line(out, "  THE REST OF THE BILL:");
        line(out, "    the library is about " + jarMegabytes() + " MB");
        line(out, "    registering it is global: every library can use it");
        line(out, "    names moved from jdk15on to jdk18on: upgrade with care");
        line(out, "    the JDK cannot make a certificate and has no CMS");
    }

    private static int encodedLength(X509Certificate cert) {
        try {
            return cert.getEncoded().length;
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String algorithmName(String oid) {
        return switch (oid) {
            case "1.2.840.113549.1.1.7" -> "RSA-OAEP";
            case "1.2.840.113549.1.1.1" -> "RSA with PKCS#1 v1.5";
            default -> oid;
        };
    }

    private static long jarMegabytes() throws IOException {
        try {
            File jar = new File(BouncyCastleProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Math.round(jar.length() / (1024.0 * 1024.0));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static String wrongPassword(String locked) {
        try {
            Pem.readEncryptedPrivateKey(locked, "wrong".toCharArray());
            return "it read";
        } catch (Pem.WrongPasswordException e) {
            return "refused";
        }
    }

    private static String eveResult(KeyPair eve, X509Certificate cert, byte[] envelope) {
        try {
            CmsEnvelope.open(eve.getPrivate(), cert, envelope);
            return "opened";
        } catch (CmsEnvelope.NotForThisKeyException e) {
            return "refused";
        }
    }
}
