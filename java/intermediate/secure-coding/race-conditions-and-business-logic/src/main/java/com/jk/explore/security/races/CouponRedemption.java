package com.jk.explore.security.races;

/**
 * Redeems a single-use coupon at checkout. The naive way checks that it is unused, applies the discount, then marks it used: three
 * steps. The safe way claims it first, in one step, and applies the discount only if the claim was this request's.
 */
final class CouponRedemption implements Steps {

    private final String name;
    private final CouponBook book;
    private final String code;
    private final long discount;
    private final boolean claimFirst;
    private int next;
    private boolean allowed;
    private String outcome = "";

    CouponRedemption(String name, CouponBook book, String code, long discount, boolean claimFirst) {
        this.name = name;
        this.book = book;
        this.code = code;
        this.discount = discount;
        this.claimFirst = claimFirst;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String step() {
        if (claimFirst) {
            next = 3;
            allowed = book.claim(code);
            if (allowed) {
                book.give(discount);
                outcome = "discount " + discount;
                return name + " claims " + code + " and gets " + discount + " off";
            }
            outcome = "refused";
            return name + " claims " + code + ": already used, refused";
        }
        switch (next++) {
            case 0 -> {
                allowed = !book.isUsed(code);
                return name + " checks that " + code + " is unused: " + (allowed ? "yes" : "no");
            }
            case 1 -> {
                if (allowed) {
                    book.give(discount);
                    outcome = "discount " + discount;
                    return name + " takes " + discount + " off the order";
                }
                outcome = "refused";
                return name + " refuses the coupon";
            }
            default -> {
                book.markUsed(code);
                return name + " marks " + code + " used";
            }
        }
    }

    @Override
    public boolean waiting() {
        return false;
    }

    @Override
    public boolean finished() {
        return next >= 3;
    }

    @Override
    public String outcome() {
        return outcome;
    }
}
