package com.jk.explore.security.iammodel;

import java.util.List;
import java.util.Map;

/** A role: a name in an account, a trust policy (who may assume it, under which conditions), the policies it grants and an optional permission boundary. */
public record Role(String account, String name, Policy trust, List<Policy> policies, Policy boundary) {

    public String arn() {
        return "arn:aws:iam::" + account + ":role/" + name;
    }

    /** What a successful assumption yields. */
    public record Session(Decision decision, Principal principal) {
    }

    /**
     * sts:AssumeRole: the role's trust policy is the resource policy of the role. The caller's own policies matter too when the trust policy names only an account.
     * The session carries the role's policies, its boundary, the tags and, if given, a session policy that can only narrow what the role allows.
     */
    public Session assume(Principal caller, Map<String, String> context, Policy sessionPolicy, String sessionName) {
        Request r = new Request(caller, "sts:AssumeRole", arn(), context);
        Decision d = Evaluator.decide(r, Evaluator.Guard.of(trust, account));
        if (d.why().startsWith("explicit deny")) {
            return new Session(d, null);
        }
        Map<String, String> ctx = new java.util.LinkedHashMap<>(context);
        ctx.putIfAbsent("aws:PrincipalArn", caller.arn());
        Request withContext = new Request(caller, "sts:AssumeRole", arn(), ctx);
        if (!Evaluator.matches(trust, withContext, true, false)) {
            return new Session(new Decision(false, "implicit deny: the trust policy does not allow it"), null);
        }
        boolean namesCaller = trust.statements().stream().anyMatch(st -> st.allow() && st.principals().contains(caller.arn()));
        if (!(namesCaller && caller.account().equals(account))) {
            Decision own = Evaluator.decide(r, Evaluator.Guard.none(caller.account()));
            if (!own.allowed()) {
                return new Session(new Decision(false, "implicit deny: the trust policy names the account, so the caller's own policies must allow it too"), null);
            }
        }
        String sessionArn = "arn:aws:sts::" + account + ":assumed-role/" + name + "/" + sessionName;
        return new Session(new Decision(true, namesCaller ? "the trust policy names the caller" : "the trust policy and the caller's policies allow it"), new Principal(account, sessionArn, policies, boundary, sessionPolicy, Map.of()));
    }
}
