package com.jk.explore.security.leastprivilegespring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Refunds, guarded by method security: who may create one, who may approve one, and who may read one. */
@Service
public class Refunds {

    /** A refund request: who asked, and who approved it, if anyone. */
    public record Refund(String id, String requester, String approver) {
    }

    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();
    private final AtomicInteger next = new AtomicInteger(1);
    private final AtomicInteger reads = new AtomicInteger();

    @PreAuthorize("hasAuthority('refunds:create')")
    public Refund create() {
        String id = "R-" + next.getAndIncrement();
        Refund refund = new Refund(id, SecurityContextHolder.getContext().getAuthentication().getName(), null);
        refunds.put(id, refund);
        return refund;
    }

    /** Needs the approve permission, and the approver must not be the person who asked: a rule no role can switch off. */
    @PreAuthorize("hasAuthority('refunds:approve') and @refundRules.differentPerson(#id, authentication)")
    public Refund approve(String id) {
        Refund refund = refunds.get(id);
        Refund approved = new Refund(id, refund.requester(), SecurityContextHolder.getContext().getAuthentication().getName());
        refunds.put(id, approved);
        return approved;
    }

    /** The method runs first, and the answer is withheld afterwards unless the caller asked for it or is an approver. */
    @PostAuthorize("returnObject.requester() == authentication.name or hasAuthority('refunds:approve')")
    public Refund view(String id) {
        reads.incrementAndGet();
        return refunds.get(id);
    }

    boolean exists(String id) {
        return refunds.containsKey(id);
    }

    String requesterOf(String id) {
        Refund refund = refunds.get(id);
        return refund == null ? null : refund.requester();
    }

    int timesViewMethodRan() {
        return reads.get();
    }
}
