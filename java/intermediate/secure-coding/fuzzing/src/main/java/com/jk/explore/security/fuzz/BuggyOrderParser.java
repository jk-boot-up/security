package com.jk.explore.security.fuzz;

import java.util.ArrayList;
import java.util.List;

/**
 * The first version of the supplier order file parser. It works on every file the supplier's tool writes, and has five bugs that only odd files reach:
 * a number that is not a number, a missing fractional part, a unit suffix without its letters, an overflow that makes a total negative, and a
 * directive whose loop never ends for odd values. The {@code cov.hit} calls are the instrumentation a coverage-guided fuzzer reads.
 * <pre>
 * ORDER v2
 * # comment
 * @discount 10%
 * KTL-001;3;24.50
 * FLR-002;5kg;1.20
 * </pre>
 */
public final class BuggyOrderParser implements OrderParser {

    @Override
    public List<OrderLine> parse(String text, Coverage cov) throws ParseException {
        String[] lines = text.split("\n", -1);
        cov.hit(1);
        if (!lines[0].equals("ORDER v2")) {
            throw new ParseException("missing header");
        }
        cov.hit(2);
        List<OrderLine> out = new ArrayList<>();
        int discount = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                cov.hit(3);
                continue;
            }
            if (line.startsWith("#")) {
                cov.hit(4);
                continue;
            }
            if (line.startsWith("@")) {
                cov.hit(5);
                if (line.startsWith("@discount ")) {
                    cov.hit(6);
                    discount = Integer.parseInt(line.substring(10, line.length() - 1)); // bug 1: not a number, or no % at the end
                    cov.hit(7);
                } else if (line.startsWith("@repeat ")) {
                    cov.hit(8);
                    int n = Integer.parseInt(line.substring(8).trim());
                    while (n != 0) { // bug 2: an odd number never reaches zero
                        cov.hit(9);
                        n -= 2;
                    }
                } else {
                    throw new ParseException("unknown directive");
                }
                continue;
            }
            cov.hit(10);
            String[] f = line.split(";");
            if (f.length != 3) {
                throw new ParseException("a line needs three fields");
            }
            cov.hit(11);
            String qty = f[1];
            if (qty.endsWith("g")) {
                cov.hit(12);
                qty = qty.substring(0, qty.indexOf("kg")); // bug 3: "5g" has no "kg"
            }
            if (qty.isEmpty() || !qty.chars().allMatch(Character::isDigit)) {
                throw new ParseException("bad quantity");
            }
            cov.hit(13);
            String[] price = f[2].split("\\.");
            cov.hit(14);
            int unit = Integer.parseInt(price[0]) * 100 + Integer.parseInt(price[1]); // bug 4: "12." has no fractional part
            cov.hit(15);
            int total = Integer.parseInt(qty) * unit * (100 - discount) / 100; // bug 5: overflows for large quantities
            if (total < 0) {
                cov.hit(16);
                throw new IllegalStateException("negative total " + total);
            }
            out.add(new OrderLine(f[0], Integer.parseInt(qty), unit, total));
        }
        return out;
    }
}
