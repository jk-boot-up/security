package com.jk.explore.security.certs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The chain check by hand must reach the JDK's verdict, and name the step the JDK's reason points to. */
class ChainStepsTest {

    private final Pki pki = new Pki();
    private final X509Certificate root = pki.root.certificate();
    private final X509Certificate middle = pki.intermediate.certificate();

    private void same(List<X509Certificate> chain, Set<BigInteger> revoked, String failure) {
        List<ChainSteps.Step> steps = ChainSteps.walk(chain, root, revoked, Pki.TODAY);
        boolean jdk = new ChainValidator(Set.of(root), revoked).validate(chain, Pki.TODAY).accepted();
        assertEquals(jdk, ChainSteps.accepted(steps));
        assertEquals(failure, ChainSteps.firstFailure(steps));
    }

    @Test
    void goodChain() {
        same(List.of(pki.server.certificate(), middle), Set.of(), "none");
    }

    @Test
    void expired() {
        same(List.of(pki.expiredServer().certificate(), middle), Set.of(), "the dates");
    }

    @Test
    void rogueRootWithTheSameName() {
        same(List.of(Pki.rogue()[1].certificate()), Set.of(), "the signature");
    }

    @Test
    void changedName() {
        same(List.of(Tamper.changeName(pki.server.certificate()), middle), Set.of(), "the signature");
    }

    @Test
    void serverKeyCannotIssue() {
        Issued forged = CertBuilder.issue(CertBuilder.Request.server("other.example", Pki.START, LocalDate.of(2026, 6, 1), 50),
                CertBuilder.newKeys(), pki.server);
        same(List.of(forged.certificate(), pki.server.certificate(), middle), Set.of(), "the CA flag");
    }

    @Test
    void revoked() {
        same(List.of(pki.server.certificate(), middle), Set.of(BigInteger.valueOf(3)), "revoked");
    }

    @Test
    void missingIntermediate() {
        same(List.of(pki.server.certificate()), Set.of(), "the issuer's name");
    }
}
