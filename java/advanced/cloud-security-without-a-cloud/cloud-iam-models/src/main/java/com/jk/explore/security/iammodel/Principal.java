package com.jk.explore.security.iammodel;

import java.util.List;
import java.util.Map;

/**
 * Who is asking: an account, an ARN, the policies attached to them, an optional permission boundary, an optional session policy (for a role session) and tags.
 * A user or a role has its own policies; a session of a role carries the role's policies and, in addition, the policy passed when it was assumed.
 */
public record Principal(String account, String arn, List<Policy> policies, Policy boundary, Policy sessionPolicy, Map<String, String> tags) {

    public static Principal user(String account, String name, Policy... policies) {
        return new Principal(account, "arn:aws:iam::" + account + ":user/" + name, List.of(policies), null, null, Map.of());
    }

    public Principal withBoundary(Policy boundary) {
        return new Principal(account, arn, policies, boundary, sessionPolicy, tags);
    }

    public Principal withTags(Map<String, String> tags) {
        return new Principal(account, arn, policies, boundary, sessionPolicy, tags);
    }

    /** A caller with no account and no policies: whoever reaches the resource over the internet. */
    public static Principal anonymous() {
        return new Principal("", "anonymous", List.of(), null, null, Map.of());
    }

    /** The role name behind a role session (arn:aws:sts::account:assumed-role/Name/session), or the plain name. */
    public String roleName() {
        String[] parts = arn.split("/");
        return arn.contains(":assumed-role/") ? parts[1] : name();
    }

    public String name() {
        return arn.substring(arn.lastIndexOf('/') + 1);
    }
}
