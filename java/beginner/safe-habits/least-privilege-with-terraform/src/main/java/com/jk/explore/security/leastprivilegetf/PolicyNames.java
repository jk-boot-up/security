package com.jk.explore.security.leastprivilegetf;

import java.util.List;

/** The policies the Terraform code defines, in the order the lesson uses them. */
final class PolicyNames {

    static final List<String> ALL = List.of("wildcard", "exporter", "admin_with_deny", "boundary", "deployer");

    private PolicyNames() {
    }
}
