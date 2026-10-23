package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SigningServiceTest {

    private static KeyPair pair;

    @BeforeAll
    static void key() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        pair = g.generateKeyPair();
    }

    private static SigningService service(int max, long window, long dual) {
        return new SigningService(data -> {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(pair.getPrivate());
            s.update(data);
            return s.sign();
        }, max, window, dual);
    }

    private static SigningService.Request req(String file, long cents, List<String> approvers, long second) {
        return new SigningService.Request(file, "alice", approvers, cents, second, PaymentFile.content(file, 2, cents));
    }

    @Test
    void signsWithinTheLimitAndTheSignatureVerifies() throws Exception {
        SigningService s = service(2, 60, 1_000);
        SigningService.Request r = req("F1", 10, List.of(), 0);
        SigningService.Result res = s.submit(r);
        assertEquals(SigningService.Decision.SIGNED, res.decision());
        assertTrue(TokenAccess.verify(pair.getPublic(), r.content(), res.signature()));
    }

    @Test
    void refusesTheThirdRequestInTheWindowAndAllowsItAfterTheWindow() throws Exception {
        SigningService s = service(2, 60, 1_000);
        s.submit(req("F1", 10, List.of(), 0));
        s.submit(req("F2", 10, List.of(), 10));
        SigningService.Result third = s.submit(req("F3", 10, List.of(), 20));
        assertEquals(SigningService.Decision.RATE_LIMITED, third.decision());
        assertNull(third.signature());
        assertEquals(SigningService.Decision.SIGNED, s.submit(req("F4", 10, List.of(), 61)).decision());
    }

    @Test
    void aLargeFileNeedsTwoApproversOtherThanTheRequester() throws Exception {
        SigningService s = service(10, 60, 1_000);
        assertEquals(SigningService.Decision.NEEDS_TWO_APPROVERS, s.submit(req("F1", 1_000, List.of(), 0)).decision());
        assertEquals(SigningService.Decision.NEEDS_TWO_APPROVERS, s.submit(req("F2", 1_000, List.of("alice", "bob"), 1)).decision());
        assertEquals(SigningService.Decision.NEEDS_TWO_APPROVERS, s.submit(req("F3", 1_000, List.of("bob", "bob"), 2)).decision());
        assertEquals(SigningService.Decision.SIGNED, s.submit(req("F4", 1_000, List.of("bob", "carol"), 3)).decision());
    }

    @Test
    void everyDecisionIsInTheAuditLog() throws Exception {
        SigningService s = service(1, 60, 1_000);
        s.submit(req("F1", 10, List.of(), 0));
        s.submit(req("F2", 10, List.of(), 1));
        s.submit(req("F3", 5_000, List.of(), 2));
        assertEquals(3, s.audit().entries().size());
        assertEquals(1, s.audit().count("signed"));
        assertEquals(1, s.audit().count("refused-rate"));
        assertEquals(1, s.audit().count("refused-approvers"));
        assertTrue(s.audit().verify());
    }
}
