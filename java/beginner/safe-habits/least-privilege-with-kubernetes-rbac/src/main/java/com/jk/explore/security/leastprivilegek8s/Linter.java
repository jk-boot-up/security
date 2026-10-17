package com.jk.explore.security.leastprivilegek8s;

import com.jk.explore.security.leastprivilegek8s.Manifests.Account;
import com.jk.explore.security.leastprivilegek8s.Manifests.Binding;
import com.jk.explore.security.leastprivilegek8s.Manifests.Role;
import com.jk.explore.security.leastprivilegek8s.Manifests.Rule;
import java.util.ArrayList;
import java.util.List;

/** Flags the RBAC habits that cause incidents: cluster-admin bindings, wildcards, escalation verbs and automounted tokens. */
final class Linter {

    record Finding(String object, String problem) {
        @Override
        public String toString() {
            return object + ": " + problem;
        }
    }

    private Linter() {
    }

    static List<Finding> check(Manifests manifests) {
        List<Finding> findings = new ArrayList<>();
        for (Binding binding : manifests.bindings()) {
            if ("cluster-admin".equals(binding.roleName())) {
                findings.add(new Finding(binding.name(), "binds cluster-admin"));
            }
        }
        for (Role role : manifests.roles()) {
            if ("cluster-admin".equals(role.name())) {
                continue;
            }
            for (Rule rule : role.rules()) {
                if (rule.verbs().contains("*")) {
                    findings.add(new Finding(role.name(), "wildcard verbs"));
                }
                if (rule.resources().contains("*")) {
                    findings.add(new Finding(role.name(), "wildcard resources"));
                }
                for (String verb : List.of("escalate", "bind", "impersonate")) {
                    if (rule.verbs().contains(verb)) {
                        findings.add(new Finding(role.name(), "escalation verb " + verb));
                    }
                }
            }
        }
        for (Account account : manifests.accounts()) {
            if (account.tokenAutomounted()) {
                findings.add(new Finding(account.name(), "token is mounted into every pod"));
            }
        }
        return findings;
    }
}
