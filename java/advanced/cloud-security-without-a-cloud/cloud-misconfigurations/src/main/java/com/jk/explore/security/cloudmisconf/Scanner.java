package com.jk.explore.security.cloudmisconf;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.iam.model.Policy;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the account through the SDK, the way an auditor's tool does, then runs the nine rules on what it read. */
public final class Scanner {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    static final String EVERYONE = "http://acs.amazonaws.com/groups/global/AllUsers";

    private Scanner() {
    }

    public static List<Finding> scan(Cloud c) {
        return Rules.check(read(c));
    }

    /** Only reads: buckets, local policies, security groups, running servers and disks. */
    public static Snapshot read(Cloud c) {
        List<Snapshot.Bucket> buckets = new ArrayList<>();
        for (var b : c.s3().listBuckets().buckets()) {
            buckets.add(bucket(c, b.name()));
        }
        List<Snapshot.Policy> policies = new ArrayList<>();
        for (Policy p : c.iam().listPolicies(r -> r.scope("Local")).policies()) {
            String doc = c.iam().getPolicyVersion(r -> r.policyArn(p.arn()).versionId(p.defaultVersionId())).policyVersion().document();
            policies.add(new Snapshot.Policy(p.policyName(), statements(URLDecoder.decode(doc, StandardCharsets.UTF_8))));
        }
        List<Snapshot.Group> groups = new ArrayList<>();
        for (SecurityGroup g : c.ec2().describeSecurityGroups().securityGroups()) {
            if (!g.groupName().equals("default")) {
                List<Snapshot.Ingress> in = new ArrayList<>();
                for (IpPermission p : g.ipPermissions()) {
                    p.ipRanges().forEach(r -> in.add(new Snapshot.Ingress(p.fromPort(), r.cidrIp())));
                }
                groups.add(new Snapshot.Group(g.groupName(), in));
            }
        }
        List<Snapshot.Server> servers = new ArrayList<>();
        for (var r : c.ec2().describeInstances().reservations()) {
            for (Instance i : r.instances()) {
                if (!i.state().nameAsString().equals("terminated")) {
                    String name = i.tags().stream().filter(t -> t.key().equals("Name")).map(Tag::value).findFirst().orElse("unnamed");
                    servers.add(new Snapshot.Server(name, i.metadataOptions().httpTokensAsString(), i.metadataOptions().httpPutResponseHopLimit()));
                }
            }
        }
        var volumes = c.ec2().describeVolumes().volumes().stream().filter(v -> !v.stateAsString().equals("deleted")).toList();
        int encrypted = (int) volumes.stream().filter(v -> Boolean.TRUE.equals(v.encrypted())).count();
        return new Snapshot(buckets, policies, groups, servers, volumes.size(), encrypted);
    }

    private static Snapshot.Bucket bucket(Cloud c, String name) {
        String publicAcl = null;
        for (Grant g : c.s3().getBucketAcl(r -> r.bucket(name)).grants()) {
            if (EVERYONE.equals(g.grantee().uri())) {
                publicAcl = g.permissionAsString();
            }
        }
        String publicPolicy = null;
        try {
            for (Snapshot.Statement st : statements(c.s3().getBucketPolicy(r -> r.bucket(name)).policy())) {
                if (st.effect().equals("Allow") && "*".equals(st.principal())) {
                    publicPolicy = st.action();
                }
            }
        } catch (S3Exception e) {
            // no bucket policy
        }
        Boolean blocked;
        try {
            var pab = c.s3().getPublicAccessBlock(r -> r.bucket(name)).publicAccessBlockConfiguration();
            blocked = pab.blockPublicAcls() && pab.ignorePublicAcls() && pab.blockPublicPolicy() && pab.restrictPublicBuckets();
        } catch (S3Exception e) {
            blocked = null;
        }
        String encryption;
        try {
            encryption = c.s3().getBucketEncryption(r -> r.bucket(name)).serverSideEncryptionConfiguration().rules().getFirst()
                    .applyServerSideEncryptionByDefault().sseAlgorithmAsString();
        } catch (S3Exception e) {
            encryption = null;
        }
        boolean versioned = "Enabled".equals(c.s3().getBucketVersioning(r -> r.bucket(name)).statusAsString());
        return new Snapshot.Bucket(name, publicAcl, publicPolicy, blocked, encryption, versioned);
    }

    /** The statements of a policy document; a bucket policy also names a principal, "*" being everyone. */
    static List<Snapshot.Statement> statements(String json) {
        List<Snapshot.Statement> out = new ArrayList<>();
        for (JsonNode st : JSON.readTree(json).get("Statement")) {
            String principal = null;
            if (st.has("Principal")) {
                JsonNode p = st.get("Principal");
                principal = p.isString() ? p.asString() : p.has("AWS") ? text(p.get("AWS")) : p.toString();
            }
            out.add(new Snapshot.Statement(st.get("Effect").asString(), principal, text(st.get("Action")), text(st.get("Resource"))));
        }
        return out;
    }

    private static String text(JsonNode n) {
        if (n.isArray()) {
            List<String> parts = new ArrayList<>();
            n.forEach(x -> parts.add(x.asString()));
            return String.join(",", parts);
        }
        return n.asString();
    }
}
