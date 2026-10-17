package com.jk.explore.security.leastprivilegetf;

import java.util.ArrayList;
import java.util.List;

/** Reads the text of a policy, not its effect, and flags the shapes that go wrong: wildcards, NotAction and ways to grow one's own power. */
final class Linter {

    record Finding(String code, String sid, String why) {
    }

    /** Actions that let a holder give themselves, or someone else, more power. */
    static final List<String> ESCALATION_ACTIONS = List.of("iam:PassRole", "iam:CreateUser", "iam:AttachUserPolicy",
            "iam:PutUserPolicy", "iam:CreatePolicyVersion", "sts:AssumeRole");

    private Linter() {
    }

    static List<Finding> check(PolicyDocument policy) {
        List<Finding> findings = new ArrayList<>();
        for (PolicyDocument.Statement statement : policy.statements()) {
            if (!statement.allow()) {
                continue;
            }
            if (statement.actions().contains("*")) {
                findings.add(new Finding("ACTION_STAR", statement.sid(), "allows every action"));
            }
            for (String action : statement.actions()) {
                if (action.endsWith(":*")) {
                    findings.add(new Finding("SERVICE_STAR", statement.sid(), "allows every action of " + action.substring(0, action.length() - 2)));
                }
            }
            if (!statement.notActions().isEmpty()) {
                findings.add(new Finding("NOT_ACTION", statement.sid(), "allows everything except a list, so new actions are allowed automatically"));
            }
            if (statement.resources().contains("*")) {
                findings.add(new Finding("RESOURCE_STAR", statement.sid(), "applies to every resource"));
            }
            for (String escalation : ESCALATION_ACTIONS) {
                boolean granted = statement.actions().stream().anyMatch(p -> Glob.matches(p, escalation, true));
                if (granted && statement.resources().contains("*") && !statement.actions().contains("*")) {
                    findings.add(new Finding("ESCALATION", statement.sid(), escalation + " on every resource lets its holder pick a more powerful role"));
                }
            }
        }
        return List.copyOf(findings);
    }

    static long count(List<Finding> findings, String code) {
        return findings.stream().filter(f -> f.code().equals(code)).count();
    }
}
