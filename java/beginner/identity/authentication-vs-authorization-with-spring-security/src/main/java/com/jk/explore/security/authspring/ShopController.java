package com.jk.explore.security.authspring;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The store's order endpoints. The {@code @PreAuthorize} lines are the authorization rules: each is checked
 * before the method runs, once method security is switched on. {@code export} deliberately has none.
 */
@RestController
public class ShopController {

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN') or @orders.isOwner(#id, authentication.name)")
    public String view(@PathVariable String id) {
        return "order " + id;
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @orders.isOwner(#id, authentication.name)")
    public String cancel(@PathVariable String id) {
        return "cancelled " + id;
    }

    @PostMapping("/orders/{id}/refund")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')")
    public String refund(@PathVariable String id) {
        return "refunded " + id;
    }

    /** No rule here. Whoever the filter chain lets through can export every order. */
    @GetMapping("/orders/export")
    public String export() {
        return "every order";
    }
}
