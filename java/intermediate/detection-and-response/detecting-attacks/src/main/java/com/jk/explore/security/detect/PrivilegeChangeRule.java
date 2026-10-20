package com.jk.explore.security.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Granting the admin role is rare and always approved: alert when the actor is not an approver, or when the grant has no change ticket. */
public final class PrivilegeChangeRule implements Rule {

    private final Set<String> approvers;
    private final boolean requireTicket;

    public PrivilegeChangeRule(Set<String> approvers, boolean requireTicket) {
        this.approvers = approvers;
        this.requireTicket = requireTicket;
    }

    @Override
    public String name() {
        return "privilege-change";
    }

    @Override
    public List<Alert> evaluate(List<Event> log) {
        List<Alert> alerts = new ArrayList<>();
        for (Event e : log) {
            if (!e.type().equals("role-grant") || !e.role().equals("admin")) {
                continue;
            }
            boolean byApprover = approvers.contains(e.actor());
            boolean ticketed = !e.ticket().isEmpty();
            if (!byApprover || (requireTicket && !ticketed)) {
                alerts.add(new Alert(name(), e.user(), e.time(), "admin granted to " + e.user() + " by " + e.actor() + (byApprover ? "" : " (not an approver)") + (ticketed ? "" : ", no ticket"), List.of(e)));
            }
        }
        return alerts;
    }
}
