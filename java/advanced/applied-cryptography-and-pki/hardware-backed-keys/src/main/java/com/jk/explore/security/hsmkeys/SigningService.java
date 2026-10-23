package com.jk.explore.security.hsmkeys;

import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The bank's own rules in front of the key: at most N signatures in a window, and a payment file over a limit needs two approvers who are not
 * the person who asked. The token signs whatever it is asked; these rules live in this process.
 */
final class SigningService {

    /** Signs bytes: the token key, or in the tests a software key. */
    interface Signer {
        byte[] sign(byte[] data) throws GeneralSecurityException;
    }

    enum Decision { SIGNED, RATE_LIMITED, NEEDS_TWO_APPROVERS }

    record Request(String file, String requester, List<String> approvers, long totalCents, long second, byte[] content) {
    }

    record Result(Decision decision, byte[] signature) {
    }

    private final Signer signer;
    private final int maxPerWindow;
    private final long windowSeconds;
    private final long dualControlCents;
    private final Deque<Long> recent = new ArrayDeque<>();
    private final AuditLog audit = new AuditLog();

    SigningService(Signer signer, int maxPerWindow, long windowSeconds, long dualControlCents) {
        this.signer = signer;
        this.maxPerWindow = maxPerWindow;
        this.windowSeconds = windowSeconds;
        this.dualControlCents = dualControlCents;
    }

    AuditLog audit() {
        return audit;
    }

    Result submit(Request r) throws GeneralSecurityException {
        while (!recent.isEmpty() && recent.peekFirst() <= r.second() - windowSeconds) {
            recent.removeFirst();
        }
        if (r.totalCents() >= dualControlCents && r.approvers().stream().filter(a -> !a.equals(r.requester())).distinct().count() < 2) {
            audit.append("refused-approvers", r.file(), r.content());
            return new Result(Decision.NEEDS_TWO_APPROVERS, null);
        }
        if (recent.size() >= maxPerWindow) {
            audit.append("refused-rate", r.file(), r.content());
            return new Result(Decision.RATE_LIMITED, null);
        }
        byte[] sig = signer.sign(r.content());
        recent.addLast(r.second());
        audit.append("signed", r.file(), r.content());
        return new Result(Decision.SIGNED, sig);
    }
}
