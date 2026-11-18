package com.jk.explore.security.cloudmisconf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** The nine rules, run on what the scanner read. No SDK and no emulator here, so every rule is tested on plain values. */
public final class Rules {

    /** Ports that are for administrators and databases, never for the whole internet. */
    static final Set<Integer> ADMIN_PORTS = Set.of(22, 3389, 5432, 3306, 6379, 27017, 9200);
    static final String WORLD = "0.0.0.0/0";

    private Rules() {
    }

    /** Every finding, worst first; findings of the same severity keep the order of the account. */
    public static List<Finding> check(Snapshot s) {
        List<Finding> out = new ArrayList<>();
        for (Snapshot.Bucket b : s.buckets()) {
            if (b.publicAcl() != null) {
                out.add(new Finding("s3-public-acl", "CRITICAL", b.name(), "the ACL grants " + b.publicAcl() + " to everyone"));
            }
            if (b.publicPolicy() != null) {
                out.add(new Finding("s3-public-policy", "CRITICAL", b.name(), "the bucket policy allows " + b.publicPolicy() + " to everyone"));
            }
            if (!Boolean.TRUE.equals(b.blocked())) {
                out.add(new Finding("s3-no-public-access-block", "HIGH", b.name(), "nothing stops a public ACL or policy"));
            }
            if (b.encryption() == null) {
                out.add(new Finding("s3-no-encryption", "MEDIUM", b.name(), "no default encryption at rest"));
            }
            if (!b.versioned()) {
                out.add(new Finding("s3-no-versioning", "LOW", b.name(), "a deleted object cannot be recovered"));
            }
        }
        for (Snapshot.Policy p : s.policies()) {
            for (Snapshot.Statement st : p.statements()) {
                if (st.effect().equals("Allow") && st.action().equals("*")
                        && st.resource().equals("*")) {
                    out.add(new Finding("iam-wildcard-admin", "CRITICAL", p.name(),
                            "allows every action on every resource"));
                }
            }
        }
        for (Snapshot.Group g : s.groups()) {
            for (Snapshot.Ingress i : g.ingress()) {
                if (i.cidr().equals(WORLD) && ADMIN_PORTS.contains(i.port())) {
                    out.add(new Finding("sg-world-admin-port", "HIGH", "group " + g.name(), "port " + i.port() + " is open to " + WORLD));
                }
            }
        }
        for (Snapshot.Server v : s.servers()) {
            if (v.httpTokens().equals("optional")) {
                out.add(new Finding("ec2-imdsv1", "HIGH", "server " + v.name(), "the metadata service answers without a token"));
            }
        }
        int unencrypted = s.disks() - s.encryptedDisks();
        if (unencrypted > 0) {
            out.add(new Finding("ebs-unencrypted", "MEDIUM", unencrypted + (unencrypted == 1 ? " disk" : " disks"), "not encrypted at rest"));
        }
        out.sort(Comparator.comparingInt(Finding::rank));
        return out;
    }
}
