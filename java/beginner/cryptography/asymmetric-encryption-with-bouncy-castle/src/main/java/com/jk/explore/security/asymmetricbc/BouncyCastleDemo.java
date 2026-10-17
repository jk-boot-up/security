package com.jk.explore.security.asymmetricbc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Runs the online bank story in five numbered acts, with Bouncy Castle and OpenSSL, and prints facts that do not depend on random keys. */
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

    static byte[] statement() {
        byte[] statement = new byte[10_000];
        Arrays.fill(statement, (byte) 'x');
        return statement;
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text. Act four needs the openssl command-line tool, so its test is tagged "openssl". */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        KeyPair ana = newKeyPair();
        act1Pem(out, ana);
        X509Certificate cert = act2Certificate(out, ana);
        byte[] envelope = act3Envelope(out, ana, cert);
        act4OpenSsl(out, ana, cert, envelope);
        act5Bill(out, cert);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }

    private static void act1Pem(StringBuilder out, KeyPair ana) {
        line(out, "ONE. Keys as PEM text.");
        String pub = Pem.publicKey(ana.getPublic());
        String priv = Pem.privateKeyInTheClear(ana.getPrivate());
        line(out, "  Ana's public key as text begins: " + firstLine(pub));
        line(out, "  her private key as text begins: " + firstLine(priv) + "  <- ANYONE WHO READS THE FILE HAS THE KEY");
        line(out, "  the public key reads back to the same key: " + Pem.readPublicKey(pub).equals(ana.getPublic()));
        String locked = Pem.privateKeyEncrypted(ana.getPrivate(), PASSWORD);
        line(out, "  encrypted with a password it begins: " + firstLine(locked));
        line(out, "  with the right password it reads back: " + Pem.readEncryptedPrivateKey(locked, PASSWORD).equals(ana.getPrivate()));
        line(out, "  with a wrong password: " + wrongPassword(locked));
    }

    private static X509Certificate act2Certificate(StringBuilder out, KeyPair ana) {
        line(out, "TWO. A certificate for the public key.");
        X509Certificate cert = Certificates.selfSigned("Ana", ana);
        line(out, "  subject: " + cert.getSubjectX500Principal().getName());
        line(out, "  valid from " + cert.getNotBefore().toInstant() + " to " + cert.getNotAfter().toInstant());
        line(out, "  signed with " + cert.getSigAlgName());
        line(out, "  issuer equals subject: " + cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())
                + "  <- ANYONE CAN MAKE ONE, SO IT PROVES NOTHING YET");
        line(out, "  the certificate as text begins: " + firstLine(Pem.certificate(cert)));
        return cert;
    }

    private static byte[] act3Envelope(StringBuilder out, KeyPair ana, X509Certificate cert) {
        line(out, "THREE. A standard envelope: CMS EnvelopedData.");
        byte[] statement = statement();
        byte[] envelope = CmsEnvelope.seal(cert, statement);
        line(out, "  the bank seals a " + statement.length + "-byte statement for Ana's certificate: AES-256 for the data, RSA-OAEP for the key");
        line(out, "  Ana opens it: " + Arrays.equals(CmsEnvelope.open(ana.getPrivate(), cert, envelope), statement));
        KeyPair eve = newKeyPair();
        line(out, "  Eve tries her own private key: " + eveResult(eve, cert, envelope));
        return envelope;
    }

    private static void act4OpenSsl(StringBuilder out, KeyPair ana, X509Certificate cert, byte[] envelope)
            throws IOException, InterruptedException {
        line(out, "FOUR. A different program opens it.");
        if (!OpenSsl.available()) {
            line(out, "  openssl is not installed, so this check was skipped");
            return;
        }
        OpenSsl.Result result = OpenSsl.decrypt(envelope, Pem.privateKeyInTheClear(ana.getPrivate()), Pem.certificate(cert));
        line(out, "  the openssl command-line tool, written by other people, is given the envelope, the key file and the certificate");
        line(out, "  it opens the statement: " + Arrays.equals(result.output(), statement()) + "  <- A REAL STANDARD, NOT ONLY THIS LIBRARY");
    }

    private static void act5Bill(StringBuilder out, X509Certificate cert) throws IOException {
        line(out, "FIVE. The bill: what Bouncy Castle costs.");
        byte[] oaep = CmsEnvelope.seal(cert, Hex.bytes("x"), true);
        byte[] easy = CmsEnvelope.seal(cert, Hex.bytes("x"), false);
        line(out, "  the envelope above locks the key with " + algorithmName(CmsEnvelope.keyTransportAlgorithm(oaep)));
        line(out, "  the simple call that most examples show locks it with " + algorithmName(CmsEnvelope.keyTransportAlgorithm(easy))
                + "  <- THE EASY CALL PICKS THE OLDER PADDING");
        line(out, "  the library is about " + jarMegabytes() + " MB, and registering it changes every library in the program");
        line(out, "  its package names changed from jdk15on to jdk18on, so upgrades need care");
        line(out, "  the JDK alone could do the maths, but not the PEM, the certificate or the CMS format");
    }

    private static String algorithmName(String oid) {
        return switch (oid) {
            case "1.2.840.113549.1.1.7" -> "RSA-OAEP (" + oid + ")";
            case "1.2.840.113549.1.1.1" -> "RSA with PKCS#1 v1.5 padding (" + oid + ")";
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
