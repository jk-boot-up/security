package com.jk.explore.security.iammodel;

import java.util.List;
import java.util.Map;

/** The condition operators of policy documents, with the rules for a key that is missing from the request, IfExists, and policy variables such as ${aws:username}. */
final class Conditions {

    private Conditions() {
    }

    static boolean holds(Map<String, Map<String, List<String>>> conditions, Map<String, String> context) {
        for (Map.Entry<String, Map<String, List<String>>> op : conditions.entrySet()) {
            for (Map.Entry<String, List<String>> key : op.getValue().entrySet()) {
                if (!one(op.getKey(), key.getKey(), key.getValue(), context)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String lookup(Map<String, String> context, String key) {
        for (Map.Entry<String, String> e : context.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    static String substitute(String value, Map<String, String> context) {
        String out = value;
        int at;
        while ((at = out.indexOf("${")) >= 0 && out.indexOf('}', at) > at) {
            String key = out.substring(at + 2, out.indexOf('}', at));
            String v = lookup(context, key);
            out = out.substring(0, at) + (v == null ? "" : v) + out.substring(out.indexOf('}', at) + 1);
        }
        return out;
    }

    private static boolean one(String operator, String key, List<String> values, Map<String, String> context) {
        String actual = lookup(context, key);
        boolean ifExists = operator.endsWith("IfExists");
        String op = ifExists ? operator.substring(0, operator.length() - "IfExists".length()) : operator;
        if (op.equals("Null")) {
            return values.contains(String.valueOf(actual == null));
        }
        boolean negated = op.startsWith("Not") || op.contains("NotEquals") || op.contains("NotLike");
        if (actual == null) {
            return ifExists || negated;
        }
        boolean any = false;
        for (String raw : values) {
            String v = substitute(raw, context);
            if (positive(op, actual, v)) {
                any = true;
                break;
            }
        }
        return negated ? !any : any;
    }

    private static boolean positive(String op, String actual, String value) {
        return switch (op) {
            case "StringEquals", "StringNotEquals" -> actual.equals(value);
            case "StringEqualsIgnoreCase", "StringNotEqualsIgnoreCase" -> actual.equalsIgnoreCase(value);
            case "StringLike", "StringNotLike" -> Wild.matches(value, actual, false);
            case "ArnEquals", "ArnLike", "ArnNotEquals", "ArnNotLike" -> Wild.matches(value, actual, false);
            case "Bool" -> actual.equalsIgnoreCase(value);
            case "NumericLessThan" -> Double.parseDouble(actual) < Double.parseDouble(value);
            case "NumericGreaterThanEquals" -> Double.parseDouble(actual) >= Double.parseDouble(value);
            case "IpAddress", "NotIpAddress" -> inCidr(actual, value);
            default -> throw new IllegalArgumentException("unknown condition operator " + op);
        };
    }

    static boolean inCidr(String ip, String cidr) {
        String[] parts = cidr.split("/");
        long mask = parts.length == 1 ? 0xFFFFFFFFL : (0xFFFFFFFFL << (32 - Integer.parseInt(parts[1]))) & 0xFFFFFFFFL;
        return (toLong(ip) & mask) == (toLong(parts[0]) & mask);
    }

    private static long toLong(String ip) {
        long v = 0;
        for (String p : ip.split("\\.")) {
            v = (v << 8) | Integer.parseInt(p);
        }
        return v;
    }
}
