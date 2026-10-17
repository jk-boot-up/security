package com.jk.explore.security.accesscontrol;

import java.util.List;

/** The requests the demo sends: what an attacker tries, and what honest users do every day. */
final class Attempts {

    static final String ANA = "token-ana-7Q2x";
    static final String BEN = "token-ben-4Lp9";
    static final String SAM = "token-sam-2Kd6";
    static final String DAVE = "token-dave-5Ht1";

    /** One request and what it stands for. */
    record Attempt(String label, Request request) {
    }

    private Attempts() {
    }

    /** Things nobody should be able to do: each is one way access control breaks. */
    static List<Attempt> attacks() {
        return List.of(
                new Attempt("ana reads ben's order 1002 (object level, horizontal)", Request.of("GET", "/orders/1002", ANA)),
                new Attempt("ana reads ben's gift order from a leaked link", Request.of("GET", "/orders/" + ShopData.GIFT_ORDER, ANA)),
                new Attempt("ana sends ben's order 1002 to her address (horizontal)", Request.of("POST", "/orders/1002/address", ANA, "12 Palm Street")),
                new Attempt("ana cancels ben's order 1002", Request.of("DELETE", "/orders/1002", ANA)),
                new Attempt("someone signed out reads the sales report (forgotten route)", Request.of("GET", "/admin/reports", null)),
                new Attempt("ana reads the sales report (vertical)", Request.of("GET", "/admin/reports", ANA)),
                new Attempt("ana refunds order 1002 (button hidden, route open)", Request.of("POST", "/admin/refunds", ANA, "1002")),
                new Attempt("ana lists the users", Request.of("GET", "/admin/users", ANA)),
                new Attempt("ana deletes ben's account (DELETE has no check)", Request.of("DELETE", "/admin/users/ben", ANA)));
    }

    /** Things honest users must still be able to do after the fix. */
    static List<Attempt> honest() {
        return List.of(
                new Attempt("ana reads her own order 1001", Request.of("GET", "/orders/1001", ANA)),
                new Attempt("ana changes the address of her own order 1003", Request.of("POST", "/orders/1003/address", ANA, "4 New Street")),
                new Attempt("sam from support reads ben's order 1002", Request.of("GET", "/orders/1002", SAM)),
                new Attempt("ben cancels his own order 1002", Request.of("DELETE", "/orders/1002", BEN)),
                new Attempt("dave the admin reads the sales report", Request.of("GET", "/admin/reports", DAVE)),
                new Attempt("dave the admin refunds order 1001", Request.of("POST", "/admin/refunds", DAVE, "1001")));
    }
}
