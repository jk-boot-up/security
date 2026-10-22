package com.jk.explore.security.cloudmisconf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.iam.model.Policy;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the account through the SDK, the way an auditor's tool does, and reports what is exposed, unencrypted, over-broad or open to the world. */
public final class Scanner {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<Integer> ADMIN_PORTS = Set.of(22, 3389, 5432, 3306, 6379, 27017, 9200);

    private Scanner() {
    }

    public static List<Finding> scan(Cloud c) {
        List<Finding> out = new ArrayList<>();
        buckets(c, out);
        policies(c, out);
        groups(c, out);
        servers(c, out);
        return out;
    }

    private static void buckets(Cloud c, List<Finding> out) {
        for (var b : c.s3().listBuckets().buckets()) {
            String name = b.name();
            for (Grant g : c.s3().getBucketAcl(r -> r.bucket(name)).grants()) {
                if (g.grantee().uri() != null && g.grantee().uri().endsWith("/global/AllUsers")) {
                    out.add(new Finding("s3-public-acl", "CRITICAL", "bucket " + name, "the ACL grants " + g.permission() + " to everyone"));
                }
            }
            try {
                JsonNode policy = JSON.readTree(c.s3().getBucketPolicy(r -> r.bucket(name)).policy());
                for (JsonNode st : policy.get("Statement")) {
                    String principal = st.get("Principal").isString() ? st.get("Principal").asString() : st.get("Principal").toString();
                    if (st.get("Effect").asString().equals("Allow") && (principal.equals("*") || principal.contains("\"AWS\":\"*\""))) {
                        out.add(new Finding("s3-public-policy", "CRITICAL", "bucket " + name, "the bucket policy allows " + st.get("Action").toString().replace("\"", "") + " to everyone"));
                    }
                }
            } catch (S3Exception e) {
                // no bucket policy
            }
            try {
                var pab = c.s3().getPublicAccessBlock(r -> r.bucket(name)).publicAccessBlockConfiguration();
                if (!(pab.blockPublicAcls() && pab.ignorePublicAcls() && pab.blockPublicPolicy() && pab.restrictPublicBuckets())) {
                    out.add(new Finding("s3-no-public-access-block", "HIGH", "bucket " + name, "public access is only partly blocked"));
                }
            } catch (S3Exception e) {
                out.add(new Finding("s3-no-public-access-block", "HIGH", "bucket " + name, "nothing stops a public ACL or policy from being added"));
            }
            try {
                c.s3().getBucketEncryption(r -> r.bucket(name));
            } catch (S3Exception e) {
                out.add(new Finding("s3-no-encryption", "MEDIUM", "bucket " + name, "no default encryption at rest"));
            }
            if (c.s3().getBucketVersioning(r -> r.bucket(name)).status() == null) {
                out.add(new Finding("s3-no-versioning", "LOW", "bucket " + name, "an overwritten or deleted object cannot be recovered"));
            }
        }
    }

    private static void policies(Cloud c, List<Finding> out) {
        for (Policy p : c.iam().listPolicies(r -> r.scope("Local")).policies()) {
            String doc = java.net.URLDecoder.decode(c.iam().getPolicyVersion(r -> r.policyArn(p.arn()).versionId(p.defaultVersionId())).policyVersion().document(), java.nio.charset.StandardCharsets.UTF_8);
            for (JsonNode st : JSON.readTree(doc).get("Statement")) {
                boolean allActions = st.get("Action").toString().replace("\"", "").replace("[", "").replace("]", "").equals("*");
                boolean allResources = st.get("Resource").toString().replace("\"", "").equals("*");
                if (st.get("Effect").asString().equals("Allow") && allActions && allResources) {
                    out.add(new Finding("iam-wildcard-admin", "CRITICAL", "policy " + p.policyName(), "allows every action on every resource"));
                }
            }
        }
    }

    private static void groups(Cloud c, List<Finding> out) {
        for (SecurityGroup g : c.ec2().describeSecurityGroups().securityGroups()) {
            if (g.groupName().equals("default")) {
                continue;
            }
            for (IpPermission p : g.ipPermissions()) {
                boolean world = p.ipRanges().stream().anyMatch(r -> r.cidrIp().equals("0.0.0.0/0"));
                if (world && p.fromPort() != null && ADMIN_PORTS.contains(p.fromPort())) {
                    out.add(new Finding("sg-world-admin-port", "HIGH", "group " + g.groupName(), "port " + p.fromPort() + " is open to 0.0.0.0/0"));
                }
            }
        }
    }

    private static void servers(Cloud c, List<Finding> out) {
        for (var r : c.ec2().describeInstances().reservations()) {
            for (Instance i : r.instances()) {
                if (i.state().nameAsString().equals("terminated")) {
                    continue;
                }
                if (i.metadataOptions() != null && i.metadataOptions().httpTokensAsString().equals("optional")) {
                    out.add(new Finding("ec2-imdsv1", "HIGH", "the web server", "the metadata service answers without a token (IMDSv1), so one SSRF reads the role's credentials"));
                }
            }
        }
        long unencrypted = c.ec2().describeVolumes().volumes().stream().filter(v -> !Boolean.TRUE.equals(v.encrypted())).filter(v -> !v.stateAsString().equals("deleted")).count();
        if (unencrypted > 0) {
            out.add(new Finding("ebs-unencrypted", "MEDIUM", unencrypted + (unencrypted == 1 ? " disk" : " disks"), "not encrypted at rest"));
        }
    }
}
