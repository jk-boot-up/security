package com.jk.explore.security.iammodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A search for the paths by which a principal can make itself an administrator, using only what the policies allow: assume a role, change a policy that it is attached to,
 * attach a policy to itself, or pass an administrator role to a function that it creates and runs. Each step is asked of the evaluator.
 */
public final class Escalation {

    /** Everything the search knows about: the principals, the roles, the customer-managed policies attached to them and the admin roles. */
    public record World(String account, List<Principal> users, List<Role> roles, Map<String, String> policyOf) {
    }

    private Escalation() {
    }

    static boolean can(Principal p, String action, String resource, String account) {
        return Evaluator.decide(new Request(p, action, resource), Evaluator.Guard.none(account)).allowed();
    }

    /** True if the principal may do anything to anything. */
    public static boolean isAdmin(Principal p, String account) {
        return can(p, "iam:CreateUser", "arn:aws:iam::" + account + ":user/x", account) && can(p, "s3:DeleteBucket", "arn:aws:s3:::x", account) && can(p, "kms:Decrypt", "arn:aws:kms:::key/x", account);
    }

    private static Principal sessionOf(Role r) {
        return new Principal(r.account(), "arn:aws:sts::" + r.account() + ":assumed-role/" + r.name() + "/s", r.policies(), r.boundary(), null, Map.of());
    }

    /** The ways this principal reaches administrator rights, as readable steps; at most three steps deep. */
    public static List<String> paths(Principal start, World w) {
        List<String> out = new ArrayList<>();
        search(start, w, "", 0, out);
        return out;
    }

    private static void search(Principal p, World w, String prefix, int depth, List<String> out) {
        String name = prefix.isEmpty() ? p.name() : prefix;
        if (depth > 0 && isAdmin(p, w.account())) {
            out.add(name + " => administrator");
            return;
        }
        for (Role admin : w.roles()) {
            if (isAdmin(sessionOf(admin), w.account()) && can(p, "iam:PassRole", admin.arn(), w.account()) && can(p, "lambda:CreateFunction", "arn:aws:lambda:::function:f", w.account())
                    && can(p, "lambda:InvokeFunction", "arn:aws:lambda:::function:f", w.account())) {
                out.add(name + " -> iam:PassRole(" + admin.name() + ") + lambda:CreateFunction + lambda:InvokeFunction => administrator");
            }
        }
        for (Map.Entry<String, String> e : w.policyOf().entrySet()) {
            String policyArn = "arn:aws:iam::" + w.account() + ":policy/" + e.getKey();
            if (e.getValue().equals(p.roleName()) || e.getValue().equals("*")) {
                if (can(p, "iam:CreatePolicyVersion", policyArn, w.account()) && depth > 0) {
                    out.add(name + " -> iam:CreatePolicyVersion on " + e.getKey() + " => administrator");
                }
            }
        }
        if (can(p, "iam:AttachUserPolicy", p.arn(), w.account()) || can(p, "iam:AttachRolePolicy", p.arn().replace("assumed-role", "role"), w.account())) {
            out.add(name + " -> iam:Attach...Policy on itself => administrator");
        }
        if (depth < 3) {
            for (Role r : w.roles()) {
                Role.Session s = r.assume(p, Map.of("aws:MultiFactorAuthPresent", "true"), null, "s");
                if (s.decision().allowed() && depth == 0 || s.decision().allowed() && !prefix.contains(r.name())) {
                    search(s.principal(), w, name + " -> assume " + r.name(), depth + 1, out);
                }
            }
        }
    }
}
