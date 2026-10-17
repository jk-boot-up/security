package com.jk.explore.security.securitylogging;

import java.util.List;
import java.util.Map;

/** Walks a hash-chained log and reports the first line that does not follow the one before it. */
final class ChainCheck {

    /** {@code brokenAt} is the 1-based number of the first line that does not follow, or 0 when the chain is intact. */
    record Result(boolean intact, int brokenAt, String head) {
    }

    private ChainCheck() {
    }

    static Result verify(List<String> lines, String anchor) {
        String expected = anchor;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Map<String, String> fields;
            try {
                fields = Json.parse(line);
            } catch (IllegalArgumentException e) {
                return new Result(false, i + 1, expected);
            }
            if (!expected.equals(fields.get("prev"))) {
                return new Result(false, i + 1, expected);
            }
            expected = Hashes.sha256(line);
        }
        return new Result(true, 0, expected);
    }
}
