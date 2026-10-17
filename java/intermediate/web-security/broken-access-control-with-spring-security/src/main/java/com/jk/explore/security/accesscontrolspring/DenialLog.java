package com.jk.explore.security.accesscontrolspring;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Every refusal, as one line: who asked and for what. It records names, methods and paths, and never credentials, headers or bodies.
 * Spring Security publishes an {@link AuthorizationDeniedEvent} for each refusal by the filter chain or by method security; refusals
 * decided by the store itself, such as someone else's order, are added by {@link OrderAccess}.
 */
@Component
public class DenialLog {

    private final List<String> lines = new CopyOnWriteArrayList<>();

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        lines.add("DENY " + who(event.getAuthentication().get()) + " " + what(event.getObject()));
    }

    void objectDenied(String who, String route, String id) {
        lines.add("DENY " + who + " " + route + ": order " + id + " is not theirs");
    }

    List<String> lines() {
        return List.copyOf(lines);
    }

    private static String who(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "someone signed out";
        }
        return authentication.getName();
    }

    private static String what(Object object) {
        if (object instanceof HttpServletRequest request) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        if (object instanceof MethodInvocationResult result) {
            return method(result.getMethodInvocation());
        }
        if (object instanceof MethodInvocation invocation) {
            return method(invocation);
        }
        return object.getClass().getSimpleName();
    }

    private static String method(MethodInvocation invocation) {
        return invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName();
    }
}
