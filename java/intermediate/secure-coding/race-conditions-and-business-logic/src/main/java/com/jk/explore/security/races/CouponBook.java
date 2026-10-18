package com.jk.explore.security.races;

import java.util.HashSet;
import java.util.Set;

/** Single-use coupons in the step-by-step model, and the total discount the store has given. */
final class CouponBook {

    private final Set<String> used = new HashSet<>();
    private long discountGiven;

    boolean isUsed(String code) {
        return used.contains(code);
    }

    void markUsed(String code) {
        used.add(code);
    }

    /** Marks the coupon used and reports whether this call was the first: check and mark in one step. */
    boolean claim(String code) {
        return used.add(code);
    }

    void give(long discount) {
        discountGiven += discount;
    }

    long discountGiven() {
        return discountGiven;
    }
}
