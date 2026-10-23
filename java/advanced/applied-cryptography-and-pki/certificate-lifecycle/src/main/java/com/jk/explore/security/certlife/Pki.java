package com.jk.explore.security.certlife;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A private certificate authority made with openssl: a self-signed root that signs an intermediate, and the intermediate
 * that signs the certificates (openssl ca, with its index.txt database). Keys stay in the work directory.
 */
final class Pki {

    /** A certificate that was issued, with its files. */
    record Issued(Path certFile, Path keyFile, X509Certificate cert) {
    }

    private static final DateTimeFormatter ASN1 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    private final Openssl ssl;
    private final Path dir;
    private final Path rootCert;
    private final Path intCert;
    private final Path intKey;
    private final Path config;
    private final String ocspUrl;
    private int counter;

    Pki(Openssl ssl, Path dir, String ocspUrl) throws IOException, InterruptedException, GeneralSecurityException {
        this.ssl = ssl;
        this.dir = Files.createDirectories(dir.toAbsolutePath());
        this.ocspUrl = ocspUrl;
        Files.createDirectories(dir.resolve("newcerts"));
        Path rootKey = dir.resolve("root.key");
        rootCert = dir.resolve("root.crt");
        intKey = dir.resolve("intermediate.key");
        intCert = dir.resolve("intermediate.crt");
        config = dir.resolve("ca.cnf");
        ssl.must("genpkey", "-algorithm", "EC", "-pkeyopt", "ec_paramgen_curve:P-256", "-out", rootKey.toString());
        ssl.must("req", "-x509", "-new", "-key", rootKey.toString(), "-subj", "/O=Bank/CN=Bank Root CA", "-days", "3650", "-sha256",
                "-addext", "basicConstraints=critical,CA:true", "-addext", "keyUsage=critical,keyCertSign,cRLSign", "-out", rootCert.toString());
        ssl.must("genpkey", "-algorithm", "EC", "-pkeyopt", "ec_paramgen_curve:P-256", "-out", intKey.toString());
        Path csr = dir.resolve("intermediate.csr");
        ssl.must("req", "-new", "-key", intKey.toString(), "-subj", "/O=Bank/CN=Bank Issuing CA", "-out", csr.toString());
        Path ext = dir.resolve("intermediate.ext");
        Files.writeString(ext, "basicConstraints=critical,CA:true,pathlen:0\nkeyUsage=critical,keyCertSign,cRLSign\n"
                + "subjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid\n");
        ssl.must("x509", "-req", "-in", csr.toString(), "-CA", rootCert.toString(), "-CAkey", rootKey.toString(), "-set_serial", "1",
                "-days", "1825", "-sha256", "-extfile", ext.toString(), "-out", intCert.toString());
        Files.writeString(dir.resolve("index.txt"), "");
        Files.writeString(dir.resolve("serial"), "1000\n");
        Files.writeString(dir.resolve("crlnumber"), "01\n");
        Files.writeString(config, """
                [ca]
                default_ca = CA_default
                [CA_default]
                database = %1$s/index.txt
                new_certs_dir = %1$s/newcerts
                serial = %1$s/serial
                crlnumber = %1$s/crlnumber
                certificate = %2$s
                private_key = %3$s
                default_md = sha256
                policy = policy
                unique_subject = no
                default_days = 30
                default_crl_days = 7
                [policy]
                organizationName = optional
                commonName = supplied
                """.formatted(dir, intCert, intKey));
    }

    static String asn1(Instant t) {
        return ASN1.format(t);
    }

    Path dir() {
        return dir;
    }

    Path rootFile() {
        return rootCert;
    }

    Path intermediateFile() {
        return intCert;
    }

    Path intermediateKeyFile() {
        return intKey;
    }

    Path indexFile() {
        return dir.resolve("index.txt");
    }

    X509Certificate root() throws IOException, GeneralSecurityException {
        return readCertificates(rootCert).getFirst();
    }

    X509Certificate intermediate() throws IOException, GeneralSecurityException {
        return readCertificates(intCert).getFirst();
    }

    /** A new key and a certificate request for it; returns the key file (the request is next to it, with .csr). */
    Path newKeyAndRequest(String name) throws IOException, InterruptedException {
        Path key = dir.resolve(name + ".key");
        ssl.must("genpkey", "-algorithm", "EC", "-pkeyopt", "ec_paramgen_curve:P-256", "-out", key.toString());
        ssl.must("req", "-new", "-key", key.toString(), "-subj", "/CN=" + name, "-out", csrFor(key).toString());
        return key;
    }

    private static Path csrFor(Path key) {
        return key.resolveSibling(key.getFileName().toString().replace(".key", ".csr"));
    }

    /** Signs the request next to the key for a server: serverAuth, digitalSignature, the names, and a pointer to the OCSP responder when asked. */
    Issued issue(String cn, Path key, List<String> dnsNames, Instant notBefore, Instant notAfter, boolean withOcsp) throws IOException, InterruptedException, GeneralSecurityException {
        int n = ++counter;
        Path ext = dir.resolve("leaf-" + n + ".ext");
        StringBuilder sb = new StringBuilder("[ext]\nbasicConstraints=critical,CA:false\nkeyUsage=critical,digitalSignature\nextendedKeyUsage=serverAuth\n"
                + "subjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid\nsubjectAltName=");
        List<String> sans = new ArrayList<>();
        for (String d : dnsNames) {
            sans.add("DNS:" + d);
        }
        sb.append(String.join(",", sans)).append('\n');
        if (withOcsp) {
            sb.append("authorityInfoAccess=OCSP;URI:").append(ocspUrl).append('\n');
        }
        Files.writeString(ext, sb.toString());
        Path crt = dir.resolve("leaf-" + n + ".crt");
        ssl.must("ca", "-batch", "-config", config.toString(), "-extfile", ext.toString(), "-extensions", "ext", "-in", csrFor(key).toString(),
                "-out", crt.toString(), "-notext", "-subj", "/O=Bank/CN=" + cn, "-startdate", asn1(notBefore), "-enddate", asn1(notAfter));
        return new Issued(crt, key, readCertificates(crt).getFirst());
    }

    void revoke(Issued cert) throws IOException, InterruptedException {
        ssl.must("ca", "-config", config.toString(), "-revoke", cert.certFile().toString(), "-crl_reason", "keyCompromise");
    }

    /** Writes a CRL that the intermediate signs and that is good for the given number of seconds. */
    X509CRL generateCrl(long validSeconds) throws IOException, InterruptedException, GeneralSecurityException {
        Path out = dir.resolve("crl.pem");
        ssl.must("ca", "-config", config.toString(), "-gencrl", "-crlsec", Long.toString(validSeconds), "-out", out.toString());
        try (InputStream in = Files.newInputStream(out)) {
            return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(in);
        }
    }

    /** Marks the expired certificates in the database, so that a CRL no longer lists them. */
    void updateDatabase() throws IOException, InterruptedException {
        ssl.must("ca", "-config", config.toString(), "-updatedb");
    }

    /** An OCSP response for one certificate, signed by the intermediate, written as DER (what a server staples). */
    Path ocspResponseFor(Issued cert, String name) throws IOException, InterruptedException {
        Path respIn = dir.resolve(name + ".req");
        Path out = dir.resolve(name + ".der");
        ssl.must("ocsp", "-issuer", intCert.toString(), "-cert", cert.certFile().toString(), "-reqout", respIn.toString());
        ssl.must("ocsp", "-reqin", respIn.toString(), "-index", indexFile().toString(), "-CA", intCert.toString(), "-rsigner", intCert.toString(),
                "-rkey", intKey.toString(), "-ndays", "1", "-resp_no_certs", "-respout", out.toString());
        return out;
    }

    /** The command that runs the OCSP responder on the given port (openssl ocsp cannot choose the address to listen on). */
    List<String> responderCommand(int port) {
        return ssl.command("ocsp", "-index", indexFile().toString(), "-port", Integer.toString(port), "-CA", intCert.toString(),
                "-rsigner", intCert.toString(), "-rkey", intKey.toString(), "-ndays", "1", "-resp_no_certs");
    }

    static List<X509Certificate> readCertificates(Path file) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(file)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(c -> (X509Certificate) c).toList();
        }
    }
}
