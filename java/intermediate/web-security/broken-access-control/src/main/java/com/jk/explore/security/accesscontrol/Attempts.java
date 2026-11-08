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
                new Attempt("ana reads ben's order 1002", Request.of("GET", "/orders/1002", ANA)),
                new Attempt("ana opens ben's leaked gift link", Request.of("GET", "/orders/" + ShopData.GIFT_ORDER, ANA)),
                new Attempt("ana sends ben's order to her address", Request.of("POST", "/orders/1002/address", ANA, "12 Palm Street")),
                new Attempt("ana cancels ben's order 1002", Request.of("DELETE", "/orders/1002", ANA)),
                new Attempt("signed out, reads the sales report", Request.of("GET", "/admin/reports", null)),
                new Attempt("ana reads the sales report", Request.of("GET", "/admin/reports", ANA)),
                new Attempt("ana refunds order 1002", Request.of("POST", "/admin/refunds", ANA, "1002")),
                new Attempt("ana lists the users", Request.of("GET", "/admin/users", ANA)),
                new Attempt("ana deletes ben's account", Request.of("DELETE", "/admin/users/ben", ANA)));
    }

    /** Things honest users must still be able to do after the fix. */
    static List<Attempt> honest() {
        return List.of(
                new Attempt("ana reads her order 1001", Request.of("GET", "/orders/1001", ANA)),
                new Attempt("ana moves her order 1003", Request.of("POST", "/orders/1003/address", ANA, "4 New Street")),
                new Attempt("sam (support) reads order 1002", Request.of("GET", "/orders/1002", SAM)),
                new Attempt("ben cancels his order 1002", Request.of("DELETE", "/orders/1002", BEN)),
                new Attempt("dave (admin) reads the report", Request.of("GET", "/admin/reports", DAVE)),
                new Attempt("dave (admin) refunds order 1001", Request.of("POST", "/admin/refunds", DAVE, "1001")));
    }
}
