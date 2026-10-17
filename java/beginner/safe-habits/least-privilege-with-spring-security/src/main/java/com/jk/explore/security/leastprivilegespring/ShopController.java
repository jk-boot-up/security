package com.jk.explore.security.leastprivilegespring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** The store's endpoints. The rules that guard them are in {@link Shop}, and in annotations on {@link Refunds}. */
@RestController
public class ShopController {

    private final Refunds refunds;

    public ShopController(Refunds refunds) {
        this.refunds = refunds;
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }

    @GetMapping("/secrets")
    public String secrets() {
        return "secrets";
    }

    @GetMapping("/staff/queue")
    public String queue() {
        return "queue";
    }

    @PutMapping("/payments/{id}")
    public String pay(@PathVariable String id) {
        return "paid " + id;
    }

    @PostMapping("/refunds")
    public String create() {
        return refunds.create().id();
    }

    @PostMapping("/refunds/{id}/approve")
    public String approve(@PathVariable String id) {
        return "approved by " + refunds.approve(id).approver();
    }

    @GetMapping("/refunds/{id}")
    public String view(@PathVariable String id) {
        return "requested by " + refunds.view(id).requester();
    }
}
