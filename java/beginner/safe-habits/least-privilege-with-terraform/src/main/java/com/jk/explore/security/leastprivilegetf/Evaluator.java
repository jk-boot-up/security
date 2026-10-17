package com.jk.explore.security.leastprivilegetf;

import java.util.List;

/**
 * AWS-style evaluation for one identity: an explicit Deny in any policy wins; otherwise the request needs an Allow in the identity's
 * policies and, if a permissions boundary is set, an Allow in the boundary too; otherwise it is denied by default.
 */
final class Evaluator {

    enum Decision { ALLOW, EXPLICIT_DENY, IMPLICIT_DENY }

    private final List<PolicyDocument> identity;
    private final PolicyDocument boundary;

    Evaluator(List<PolicyDocument> identity, PolicyDocument boundary) {
        this.identity = List.copyOf(identity);
        this.boundary = boundary;
    }

    static Evaluator of(PolicyDocument... identity) {
        return new Evaluator(List.of(identity), null);
    }

    Evaluator withBoundary(PolicyDocument boundary) {
        return new Evaluator(identity, boundary);
    }

    Decision decide(String action, String resource) {
        boolean identityAllows = false;
        boolean boundaryAllows = boundary == null;
        for (PolicyDocument policy : identity) {
            for (PolicyDocument.Statement statement : policy.statements()) {
                if (applies(statement, action, resource)) {
                    if (!statement.allow()) {
                        return Decision.EXPLICIT_DENY;
                    }
                    identityAllows = true;
                }
            }
        }
        if (boundary != null) {
            for (PolicyDocument.Statement statement : boundary.statements()) {
                if (applies(statement, action, resource)) {
                    if (!statement.allow()) {
                        return Decision.EXPLICIT_DENY;
                    }
                    boundaryAllows = true;
                }
            }
        }
        return identityAllows && boundaryAllows ? Decision.ALLOW : Decision.IMPLICIT_DENY;
    }

    boolean allows(String action, String resource) {
        return decide(action, resource) == Decision.ALLOW;
    }

    static boolean applies(PolicyDocument.Statement statement, String action, String resource) {
        boolean actionMatches = statement.notActions().isEmpty()
                ? statement.actions().stream().anyMatch(p -> Glob.matches(p, action, true))
                : statement.notActions().stream().noneMatch(p -> Glob.matches(p, action, true));
        return actionMatches && statement.resources().stream().anyMatch(p -> Glob.matches(p, resource, false));
    }
}
