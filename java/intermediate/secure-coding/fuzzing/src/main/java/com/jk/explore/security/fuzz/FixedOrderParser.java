package com.jk.explore.security.fuzz;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** The same format with every input checked before it is used, and every failure reported as a {@link ParseException}. */
public final class FixedOrderParser implements OrderParser {

    private static final Pattern SKU = Pattern.compile("[A-Z]{3}-[0-9]{3}");
    private static final Pattern QTY = Pattern.compile("([0-9]{1,6})(kg)?");
    private static final Pattern PRICE = Pattern.compile("([0-9]{1,6})\\.([0-9]{2})");
    private static final Pattern PERCENT = Pattern.compile("([0-9]{1,3})%");
    private static final int MAX_LINES = 1000;

    @Override
    public List<OrderLine> parse(String text, Coverage cov) throws ParseException {
        String[] lines = text.split("\n", -1);
        cov.hit(1);
        if (!lines[0].equals("ORDER v2")) {
            throw new ParseException("missing header");
        }
        if (lines.length > MAX_LINES) {
            throw new ParseException("too many lines");
        }
        cov.hit(2);
        List<OrderLine> out = new ArrayList<>();
        int discount = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() || line.startsWith("#")) {
                cov.hit(3);
                continue;
            }
            if (line.startsWith("@")) {
                cov.hit(5);
                if (line.startsWith("@discount ")) {
                    var m = PERCENT.matcher(line.substring(10));
                    if (!m.matches() || Integer.parseInt(m.group(1)) > 100) {
                        throw new ParseException("bad discount");
                    }
                    discount = Integer.parseInt(m.group(1));
                    cov.hit(6);
                } else if (line.startsWith("@repeat ")) {
                    // the directive is accepted for compatibility and ignored: there was never a reason to loop on a number from a file
                    cov.hit(8);
                } else {
                    throw new ParseException("unknown directive");
                }
                continue;
            }
            cov.hit(10);
            String[] f = line.split(";", -1);
            if (f.length != 3 || !SKU.matcher(f[0]).matches()) {
                throw new ParseException("a line needs a SKU, a quantity and a price");
            }
            var q = QTY.matcher(f[1]);
            var p = PRICE.matcher(f[2]);
            if (!q.matches() || !p.matches()) {
                throw new ParseException("bad quantity or price");
            }
            cov.hit(13);
            int quantity = Integer.parseInt(q.group(1));
            int unit = Integer.parseInt(p.group(1)) * 100 + Integer.parseInt(p.group(2));
            long total = (long) quantity * unit * (100 - discount) / 100; // long: 999999 * 99999999 fits
            out.add(new OrderLine(f[0], quantity, unit, (int) Math.min(total, Integer.MAX_VALUE)));
        }
        return out;
    }
}
