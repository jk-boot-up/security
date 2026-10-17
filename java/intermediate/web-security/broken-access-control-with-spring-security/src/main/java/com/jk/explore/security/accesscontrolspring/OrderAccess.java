package com.jk.explore.security.accesscontrolspring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Who may see or change which order, in one place. A customer's lookups go through {@link Orders#findByIdAndOwner}, so someone else's
 * order is simply not found; staff may read any order. {@link #byLink} shows the other way, checking after the method has run.
 */
@Service
public class OrderAccess {

    private final Orders orders;
    private final DenialLog log;
    private final AtomicInteger byLinkCalls = new AtomicInteger();

    public OrderAccess(Orders orders, DenialLog log) {
        this.orders = orders;
        this.log = log;
    }

    /** An order this person may read: their own, or any order for support staff and admins. */
    Optional<Order> readable(String id, Authentication who) {
        return lookUp(id, who, "GET /orders/{id}", isAny(who, "ROLE_SUPPORT", "ROLE_ADMIN"));
    }

    /** An order this person may change the address of: only their own. */
    Optional<Order> changeable(String id, Authentication who) {
        return lookUp(id, who, "POST /orders/{id}/address", false);
    }

    /** An order this person may cancel: their own, or any order for an admin. */
    Optional<Order> cancellable(String id, Authentication who) {
        return lookUp(id, who, "DELETE /orders/{id}", isAny(who, "ROLE_ADMIN"));
    }

    /**
     * The order behind a shared link, judged after the method has run: the lookup happens, and only then is the answer withheld,
     * with a 403 that tells the caller the order exists. Kept to compare with the query-level check above.
     */
    @PostAuthorize("returnObject == null or returnObject.owner == authentication.name")
    public Order byLink(String id) {
        byLinkCalls.incrementAndGet();
        return orders.findById(id).orElse(null);
    }

    int byLinkCalls() {
        return byLinkCalls.get();
    }

    private Optional<Order> lookUp(String id, Authentication who, String route, boolean anyOrder) {
        Optional<Order> found = anyOrder ? orders.findById(id) : orders.findByIdAndOwner(id, who.getName());
        if (found.isEmpty() && orders.findById(id).isPresent()) {
            log.objectDenied(who.getName(), route, id);
        }
        return found;
    }

    private static boolean isAny(Authentication who, String... authorities) {
        for (String authority : authorities) {
            if (who.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()))) {
                return true;
            }
        }
        return false;
    }
}
