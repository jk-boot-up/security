package com.jk.explore.security.iammodel;

import java.util.Map;

/**
 * One question for the evaluator: may this principal do this action on this resource? The context holds the keys that conditions look at, such as aws:SourceIp, sts:ExternalId,
 * aws:MultiFactorAuthPresent and aws:PrincipalTag/team.
 */
public record Request(Principal principal, String action, String resource, Map<String, String> context) {

    public Request(Principal principal, String action, String resource) {
        this(principal, action, resource, Map.of());
    }

    public Request with(String key, String value) {
        Map<String, String> c = new java.util.LinkedHashMap<>(context);
        c.put(key, value);
        return new Request(principal, action, resource, c);
    }
}
