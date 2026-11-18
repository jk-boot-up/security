package com.jk.explore.security.baosecrets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A teaching class: decides a request the way an OpenBao policy does, for the two rule forms the store uses. A path that
 * ends in * covers every path that starts with what comes before it; any other path covers only itself. The most
 * specific rule that covers the request decides, and a request no rule covers is refused.
 */
final class PolicyRule {

    record Rule(String path, Set<String> capabilities) {
        boolean covers(String request) {
            if (path.endsWith("*")) {
                return request.startsWith(path.substring(0, path.length() - 1));
            }
            return request.equals(path);
        }
    }

    private static final Pattern RULE = Pattern.compile("path \"([^\"]+)\" \\{ capabilities = \\[([^]]*)]");

    private PolicyRule() {
    }

    static List<Rule> parse(String policy) {
        List<Rule> rules = new ArrayList<>();
        Matcher m = RULE.matcher(policy);
        while (m.find()) {
            rules.add(new Rule(m.group(1), Set.of(m.group(2).replace("\"", "").split(",\\s*"))));
        }
        return rules;
    }

    static boolean allows(List<Rule> rules, String request, String capability) {
        Optional<Rule> rule = rules.stream().filter(r -> r.covers(request))
                .max(Comparator.comparingInt((Rule r) -> r.path().endsWith("*") ? 0 : 1).thenComparingInt(r -> r.path().length()));
        return rule.isPresent() && rule.get().capabilities().contains(capability);
    }
}
