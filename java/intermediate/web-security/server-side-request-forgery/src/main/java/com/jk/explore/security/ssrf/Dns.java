package com.jk.explore.security.ssrf;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model of name resolution with fixed answers. A literal address (such as {@code 127.1} or {@code 2852039166}) is parsed by the JDK's own rules,
 * exactly as a real connection would, and a name is looked up in the table. A "rebinding" name answers with a public address the first time
 * it is asked and a private address every time after, the way an attacker's DNS server can.
 */
public final class Dns {

    private record Rebinding(String first, String later) {
    }

    private final Map<String, List<String>> names = new HashMap<>();
    private final Map<String, Rebinding> rebinding = new HashMap<>();
    private final Map<String, Integer> asked = new HashMap<>();

    public void add(String name, String... addresses) {
        names.put(name, List.of(addresses));
    }

    public void addRebinding(String name, String firstAnswer, String laterAnswers) {
        rebinding.put(name, new Rebinding(firstAnswer, laterAnswers));
    }

    public List<InetAddress> resolve(String host) {
        try {
            if (looksLikeLiteral(host)) {
                return List.of(InetAddress.getByName(host)); // a literal address needs no lookup
            }
            List<InetAddress> out = new ArrayList<>();
            Rebinding r = rebinding.get(host);
            if (r != null) {
                int n = asked.merge(host, 1, Integer::sum);
                out.add(InetAddress.getByName(n == 1 ? r.first() : r.later()));
                return out;
            }
            List<String> found = names.get(host);
            if (found == null) {
                throw new Unreachable("unknown host " + host);
            }
            for (String a : found) {
                out.add(InetAddress.getByName(a));
            }
            return out;
        } catch (UnknownHostException e) {
            throw new Unreachable("unknown host " + host);
        }
    }

    private static boolean looksLikeLiteral(String host) {
        return host.contains(":") || host.chars().allMatch(c -> Character.isDigit(c) || c == '.' || c == 'x' || c == 'X'
                || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) && Character.isDigit(host.charAt(0));
    }
}
