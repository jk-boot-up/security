package com.jk.explore.security.leastprivilegespring;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Separation of duties, named so that an expression can call it: {@code @refundRules.differentPerson(#id, authentication)}. */
@Component("refundRules")
public class RefundRules {

    private final Refunds refunds;

    public RefundRules(@Lazy Refunds refunds) {
        this.refunds = refunds;
    }

    public boolean differentPerson(String id, Authentication authentication) {
        String requester = refunds.requesterOf(id);
        return requester != null && !requester.equals(authentication.getName());
    }
}
