package com.jk.explore.security.accesscontrolspring;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Every route rule of the guarded store in one list, by method and path. The filter chain is built from this list, and the route
 * inventory test reads the same list, so a route that has no entry here is both refused at run time and reported by the build.
 */
final class AccessRules {

    /** One route and who may use it: anyone, anyone signed in (roles empty), or the listed roles. */
    record Rule(String method, String pattern, boolean anyone, List<String> roles) {
    }

    static final List<Rule> RULES = List.of(
            anyone("GET", "/menu"),
            signedIn("GET", "/account"),
            // signed in is all a URL rule can say about an order; whose order it is, is checked in the query (Orders.findByIdAndOwner)
            signedIn("GET", "/orders/{id}"),
            signedIn("GET", "/orders/by-link/{id}"),
            signedIn("POST", "/orders/{id}/address"),
            signedIn("DELETE", "/orders/{id}"),
            roles("GET", "/admin/users", "ADMIN"),
            roles("DELETE", "/admin/users/{name}", "ADMIN"),
            roles("POST", "/admin/refunds", "SUPPORT", "ADMIN"),
            roles("GET", "/admin/reports", "ADMIN"));

    private static Rule anyone(String method, String pattern) {
        return new Rule(method, pattern, true, List.of());
    }

    private static Rule signedIn(String method, String pattern) {
        return new Rule(method, pattern, false, List.of());
    }

    private static Rule roles(String method, String pattern, String... roles) {
        return new Rule(method, pattern, false, List.of(roles));
    }

    /** Adds one matcher per rule, in order. The caller adds {@code anyRequest().denyAll()} after them. */
    static void apply(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        for (Rule rule : RULES) {
            var matched = registry.requestMatchers(HttpMethod.valueOf(rule.method()), rule.pattern());
            if (rule.anyone()) {
                matched.permitAll();
            } else if (rule.roles().isEmpty()) {
                matched.authenticated();
            } else {
                matched.hasAnyRole(rule.roles().toArray(String[]::new));
            }
        }
    }

    static boolean covers(String method, String pattern) {
        return RULES.stream().anyMatch(r -> r.method().equals(method) && r.pattern().equals(pattern));
    }

    private AccessRules() {
    }
}
