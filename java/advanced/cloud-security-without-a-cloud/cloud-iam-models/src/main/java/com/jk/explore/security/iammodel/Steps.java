package com.jk.explore.security.iammodel;

import java.util.ArrayList;
import java.util.List;

/**
 * A teaching class: the evaluation order written out as seven questions, asked one after the other, the way the providers document it. The first answer that decides stops
 * the walk; the questions after it are not asked ("-"). A test proves that the answer is always the same as {@link Evaluator#decide}.
 */
public final class Steps {

    /** The questions, in the order they are asked. */
    public static final List<String> QUESTIONS = List.of(
            "a Deny statement matches?",
            "organization policies allow?",
            "inside the boundary?",
            "inside the session policy?",
            "an identity policy allows?",
            "the resource policy allows?",
            "same account as the resource?");

    /** The answers to {@link #QUESTIONS}, and the final ALLOW or DENY. */
    public record Walk(List<String> answers, boolean allowed) {
    }

    private Steps() {
    }

    public static Walk of(Request r, Evaluator.Guard g) {
        Request req = Evaluator.withContext(r);
        Principal p = req.principal();
        Policy resource = g.resourcePolicy();
        List<String> a = new ArrayList<>();
        // 1. An explicit deny anywhere ends it.
        String deny = denyIn(p, g, req);
        a.add(deny == null ? "no" : "yes: " + deny);
        if (deny != null) {
            return stop(a, false);
        }
        // 2. Organization policies must allow (when there are any).
        if (g.organization() == null) {
            a.add("none");
        } else {
            boolean orgAllows = g.organization().stream().anyMatch(o -> Evaluator.matches(o, req, false, false));
            a.add(orgAllows ? "yes" : "no");
            if (!orgAllows) {
                return stop(a, false);
            }
        }
        // 3, 4. A boundary and a session policy only narrow, unless the resource policy names this very principal.
        boolean named = resource != null && Evaluator.namesPrincipal(resource, req);
        for (Policy layer : new Policy[] {p.boundary(), p.sessionPolicy()}) {
            if (layer == null) {
                a.add("none");
            } else if (named) {
                a.add("skipped");
            } else {
                boolean inside = Evaluator.matches(layer, req, false, false);
                a.add(inside ? "yes" : "no");
                if (!inside) {
                    return stop(a, false);
                }
            }
        }
        // 5, 6. The two sides that can allow.
        boolean anonymous = p.account().isEmpty();
        boolean identity = !anonymous && p.policies().stream().anyMatch(pol -> Evaluator.matches(pol, req, false, false));
        boolean byResource = resource != null && Evaluator.matches(resource, req, true, false);
        a.add(anonymous ? "no account" : identity ? "yes" : "no");
        a.add(resource == null ? "none" : byResource ? "yes" : "no");
        // 7. One account: either side is enough. Two accounts (or none): both sides must allow.
        if (anonymous) {
            a.add("no account");
            return new Walk(a, byResource);
        }
        boolean same = g.resourceAccount().equals(p.account());
        a.add(same ? "yes: one side is enough" : "no: both must allow");
        return new Walk(a, same ? identity || byResource : identity && byResource);
    }

    private static String denyIn(Principal p, Evaluator.Guard g, Request req) {
        for (Policy pol : p.policies()) {
            if (Evaluator.matches(pol, req, false, true)) {
                return pol.name();
            }
        }
        if (p.boundary() != null && Evaluator.matches(p.boundary(), req, false, true)) {
            return "the boundary";
        }
        if (p.sessionPolicy() != null && Evaluator.matches(p.sessionPolicy(), req, false, true)) {
            return "the session policy";
        }
        if (g.resourcePolicy() != null && Evaluator.matches(g.resourcePolicy(), req, true, true)) {
            return g.resourcePolicy().name();
        }
        if (g.organization() != null) {
            for (Policy o : g.organization()) {
                if (Evaluator.matches(o, req, false, true)) {
                    return o.name();
                }
            }
        }
        return null;
    }

    private static Walk stop(List<String> answers, boolean allowed) {
        while (answers.size() < QUESTIONS.size()) {
            answers.add("-");
        }
        return new Walk(answers, allowed);
    }
}
