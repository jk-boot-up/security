package com.jk.explore.security.accesscontrolspring;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Refunds. The rule sits on the method, so it holds for every caller: a controller, a nightly job, or a route added next year. */
@Service
public class RefundService {

    private final Orders orders;

    public RefundService(Orders orders) {
        this.orders = orders;
    }

    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')")
    public void refund(String orderId) {
        orders.refund(orderId);
    }
}
