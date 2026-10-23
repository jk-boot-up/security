package com.jk.explore.security.meshmtls;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What a workload certificate says: who it names and for how long it is valid. Never the keys. */
public record CertInfo(String subject, Instant notBefore, Instant notAfter) {

    /** Linkerd lets a clock be 20 seconds wrong, so it backdates the start and extends the end by that much. */
    static final Duration SKEW = Duration.ofSeconds(20);

    private static final Pattern NOT_BEFORE = Pattern.compile("Not Before:\\s*(.+)");
    private static final Pattern NOT_AFTER = Pattern.compile("Not After\\s*:\\s*(.+)");
    private static final Pattern SUBJECT = Pattern.compile("Subject:\\s*CN=(\\S+)");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d HH:mm:ss yyyy", Locale.ENGLISH);

    /** Reads the first certificate in the text printed by linkerd identity. */
    public static CertInfo fromIdentityOutput(String text) {
        Matcher s = SUBJECT.matcher(text);
        Matcher b = NOT_BEFORE.matcher(text);
        Matcher a = NOT_AFTER.matcher(text);
        if (!s.find() || !b.find() || !a.find()) {
            throw new IllegalArgumentException("no certificate in: " + text);
        }
        return new CertInfo(s.group(1), when(b.group(1)), when(a.group(1)));
    }

    /** Reads a PEM certificate (a public one: a trust anchor). */
    public static CertInfo fromPem(String pem) throws CertificateException {
        X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        return new CertInfo(c.getSubjectX500Principal().getName(), c.getNotBefore().toInstant(), c.getNotAfter().toInstant());
    }

    private static Instant when(String text) {
        String cleaned = text.strip().replaceAll("\\s+", " ").replace(" UTC", "").replace(" GMT", "");
        return LocalDateTime.parse(cleaned, WHEN).toInstant(ZoneOffset.UTC);
    }

    /** How long the issuer meant it to live, without the clock-skew allowance at both ends. */
    public Duration lifetime() {
        return Duration.between(notBefore, notAfter).minus(SKEW.multipliedBy(2));
    }
}
