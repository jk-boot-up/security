package com.jk.explore.security.k8sadmission;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What the API server said to a request: admitted, or refused by which policies. */
public record Verdict(boolean admitted, List<String> policies, boolean webhookDown) {

    private static final Pattern POLICY = Pattern.compile("Policy ([a-z0-9-]+) failed");

    public static Verdict of(Sh.Result r) {
        if (r.ok()) {
            return new Verdict(true, List.of(), false);
        }
        List<String> names = new ArrayList<>();
        Matcher m = POLICY.matcher(r.output());
        while (m.find()) {
            names.add(m.group(1));
        }
        names.sort(null);
        boolean down = names.isEmpty() && (r.output().contains("failed calling webhook") || r.output().contains("no endpoints available"));
        return new Verdict(false, names, down);
    }

    @Override
    public String toString() {
        if (admitted) {
            return "admitted";
        }
        return webhookDown ? "refused: the webhook is unavailable" : policies.isEmpty() ? "RAW" : "refused by " + String.join(", ", policies);
    }
}
