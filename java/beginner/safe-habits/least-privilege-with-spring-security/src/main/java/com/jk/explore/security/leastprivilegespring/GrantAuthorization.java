package com.jk.explore.security.leastprivilegespring;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/** A custom rule: the request is allowed for someone who holds the permission, or who has an active time-limited grant for it. */
final class GrantAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private final String permission;
    private final Grants grants;

    GrantAuthorization(String permission, Grants grants) {
        this.permission = permission;
        this.grants = grants;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication who = authentication.get();
        if (who == null || !who.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        boolean held = who.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(permission::equals);
        return new AuthorizationDecision(held || grants.active(who.getName(), permission));
    }
}
