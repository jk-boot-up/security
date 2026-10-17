package com.jk.explore.security.leastprivilege;

import java.util.HashMap;
import java.util.Map;

/** Refunds need two different people: one creates, another approves. */
final class RefundDesk {

    enum Outcome { DONE, NOT_ALLOWED, SAME_PERSON, UNKNOWN_REFUND }

    private final Access access;
    private final Map<String, String> creators = new HashMap<>();
    private final Map<String, String> approvers = new HashMap<>();

    RefundDesk(Access access) {
        this.access = access;
    }

    Outcome create(String refund, String user, long tick) {
        if (!access.can(user, "refunds:create", tick)) {
            return Outcome.NOT_ALLOWED;
        }
        creators.put(refund, user);
        return Outcome.DONE;
    }

    Outcome approve(String refund, String user, long tick) {
        if (!creators.containsKey(refund)) {
            return Outcome.UNKNOWN_REFUND;
        }
        if (!access.can(user, "refunds:approve", tick)) {
            return Outcome.NOT_ALLOWED;
        }
        if (creators.get(refund).equals(user)) {
            return Outcome.SAME_PERSON;
        }
        approvers.put(refund, user);
        return Outcome.DONE;
    }

    boolean isApproved(String refund) {
        return approvers.containsKey(refund);
    }
}
