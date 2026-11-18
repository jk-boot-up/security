package com.jk.explore.security.races;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prices an order on the server. The careful checkout looks prices up in the catalogue, ignores any total the client sends, and
 * allows one coupon per order. The careless one believes the client's total and applies every coupon it is given.
 */
final class Checkout {

    record Line(String sku, int quantity) {
    }

    record Request(List<Line> lines, List<String> coupons, long totalFromClient) {
    }

    record Priced(long charged, String working, List<String> notes) {
    }

    static final Map<String, Long> CATALOGUE = Map.of("boots-42", 180L, "socks-3", 12L);
    static final Map<String, Long> COUPONS = Map.of("SUMMER20", 20L, "WELCOME20", 20L, "SPRING20", 20L);

    private Checkout() {
    }

    static Priced careless(Request request) {
        long total = request.totalFromClient();
        StringBuilder working = new StringBuilder().append(total);
        for (String coupon : request.coupons()) {
            long off = COUPONS.getOrDefault(coupon, 0L);
            total -= off;
            working.append(" - ").append(off);
        }
        return new Priced(total, working.toString(), List.of("the client's total and every coupon believed"));
    }

    static Priced careful(Request request) {
        long total = 0;
        for (Line line : request.lines()) {
            Long price = CATALOGUE.get(line.sku());
            if (price == null || line.quantity() < 1 || line.quantity() > 99) {
                return new Priced(-1, "", List.of("refused: an unknown product or a quantity outside 1 to 99"));
            }
            total += price * line.quantity();
        }
        List<String> notes = new java.util.ArrayList<>();
        if (request.totalFromClient() != total) {
            notes.add("the total sent by the client (" + request.totalFromClient() + ") is ignored");
        }
        StringBuilder working = new StringBuilder().append(total);
        Set<String> seen = new HashSet<>();
        boolean applied = false;
        for (String coupon : request.coupons()) {
            if (!COUPONS.containsKey(coupon) || !seen.add(coupon)) {
                notes.add(coupon + " refused: unknown or repeated");
            } else if (applied) {
                notes.add(coupon + " refused: one coupon per order");
            } else {
                total -= COUPONS.get(coupon);
                working.append(" - ").append(COUPONS.get(coupon));
                applied = true;
            }
        }
        return new Priced(total, working.toString(), notes);
    }
}
