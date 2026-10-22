package com.jk.explore.security.actuatorspring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The store's own endpoints. The payment key is injected the way a real store does it, so it is also in the heap. */
@RestController
class ShopController {

    private final String paymentKey;

    ShopController(@Value("${shop.payment.api-key}") String paymentKey) {
        this.paymentKey = paymentKey;
    }

    @GetMapping("/products")
    String products() {
        return "kettle, toaster, lamp";
    }

    @GetMapping("/orders/{id}")
    String order(@PathVariable int id) {
        return "order " + id + " paid with a card ending 4242 (gateway key length " + paymentKey.length() + ")";
    }

    /** A route a customer is never told about; the mappings endpoint lists it anyway. */
    @GetMapping("/internal/refunds/{id}")
    String refund(@PathVariable int id) {
        return "refund " + id + " issued";
    }
}
