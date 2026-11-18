package com.jk.explore.security.cloudmisconf;

import java.util.ArrayList;
import java.util.List;

/** What the scanner read from the account: plain values, so the rules can be tested without an emulator. */
public record Snapshot(List<Bucket> buckets, List<Policy> policies, List<Group> groups, List<Server> servers, int disks, int encryptedDisks) {

    /** A bucket: who the ACL lets read (null: nobody outside), what a policy gives everyone (null: nothing), and its three protections. */
    public record Bucket(String name, String publicAcl, String publicPolicy, Boolean blocked, String encryption, boolean versioned) {
    }

    /** One statement of a policy, with its actions and resources joined by commas; the principal is null in an identity policy. */
    public record Statement(String effect, String principal, String action, String resource) {
    }

    public record Policy(String name, List<Statement> statements) {
    }

    /** An inbound rule: a port and the addresses it is open to. */
    public record Ingress(int port, String cidr) {
    }

    public record Group(String name, List<Ingress> ingress) {
    }

    /** A server's metadata options: "optional" means version 1 still answers, "required" means a token is needed. */
    public record Server(String name, String httpTokens, int hopLimit) {
    }

    /** The snapshot as short lines, the way the demo prints it. */
    public List<String> lines() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%-16s %-7s %-6s %-11s %s", "bucket", "public", "block", "encryption", "versions"));
        for (Bucket b : buckets) {
            String open = b.publicAcl() != null ? "ACL" : b.publicPolicy() != null ? "policy" : "no";
            String block = b.blocked() == null ? "none" : b.blocked() ? "all" : "part";
            out.add(String.format("%-16s %-7s %-6s %-11s %s", b.name(), open, block, b.encryption() == null ? "none" : b.encryption(), b.versioned() ? "on" : "off"));
        }
        for (Policy p : policies) {
            for (Statement s : p.statements()) {
                out.add(String.format("%-21s %s %s on %s", p.name(), s.effect(), s.action(), s.resource().replace("arn:aws:s3:::", "")));
            }
        }
        for (Group g : groups) {
            String rules = g.ingress().isEmpty() ? "no inbound rules" : String.join(", ", g.ingress().stream().map(i -> i.port() + " from " + i.cidr()).toList());
            out.add(String.format("group %-15s %s", g.name(), rules));
        }
        for (Server s : servers) {
            out.add(String.format("server %-14s tokens %s, hop limit %d", s.name(), s.httpTokens(), s.hopLimit()));
        }
        out.add(String.format("%-21s %d, %d encrypted", "disks", disks, encryptedDisks));
        return out;
    }
}
