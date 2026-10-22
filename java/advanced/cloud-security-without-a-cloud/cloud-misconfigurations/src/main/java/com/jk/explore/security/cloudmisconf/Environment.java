package com.jk.explore.security.cloudmisconf;

import java.util.List;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.InstanceMetadataOptionsRequest;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAclRequest;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionByDefault;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionConfiguration;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;

/** The first version of the bank's account, as a team in a hurry would leave it, and the hardened version of each resource. */
public final class Environment {

    static final String POLICY_ALL = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}";
    static final String POLICY_READ = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\"],\"Resource\":\"arn:aws:s3:::bank-statements/*\"}]}";
    static final String PUBLIC_BUCKET_POLICY = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bank-exports/*\"}]}";

    private Environment() {
    }

    private static void bucket(Cloud c, String name) {
        c.s3().createBucket(CreateBucketRequest.builder().bucket(name).createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint("eu-west-1").build()).build());
    }

    /** Three buckets, two policies, two security groups, a server and a disk. */
    public static void deployFirstVersion(Cloud c) {
        bucket(c, "bank-statements");
        c.s3().putBucketAcl(PutBucketAclRequest.builder().bucket("bank-statements").acl(BucketCannedACL.PUBLIC_READ).build());
        bucket(c, "bank-exports");
        c.s3().putBucketPolicy(PutBucketPolicyRequest.builder().bucket("bank-exports").policy(PUBLIC_BUCKET_POLICY).build());
        bucket(c, "bank-audit");
        hardenBucket(c, "bank-audit");
        c.iam().createPolicy(r -> r.policyName("developer-everything").policyDocument(POLICY_ALL));
        c.iam().createPolicy(r -> r.policyName("statements-reader").policyDocument(POLICY_READ));
        String web = c.ec2().createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("web").description("web servers").build()).groupId();
        ingress(c, web, 443, "0.0.0.0/0");
        ingress(c, web, 22, "0.0.0.0/0");
        String db = c.ec2().createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("database").description("database").build()).groupId();
        ingress(c, db, 5432, "0.0.0.0/0");
        c.ec2().runInstances(RunInstancesRequest.builder().imageId("ami-03cf127a").minCount(1).maxCount(1).securityGroupIds(web)
                .metadataOptions(InstanceMetadataOptionsRequest.builder().httpTokens(HttpTokensState.OPTIONAL).httpPutResponseHopLimit(2).build()).build());
        c.ec2().createVolume(CreateVolumeRequest.builder().availabilityZone("eu-west-1a").size(8).encrypted(false).build());
    }

    static void ingress(Cloud c, String group, int port, String cidr) {
        c.ec2().authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(group)
                .ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(port).toPort(port).ipRanges(IpRange.builder().cidrIp(cidr).build()).build()).build());
    }

    /** Block all public access, encrypt by default, keep versions. */
    public static void hardenBucket(Cloud c, String name) {
        c.s3().putPublicAccessBlock(PutPublicAccessBlockRequest.builder().bucket(name).publicAccessBlockConfiguration(
                PublicAccessBlockConfiguration.builder().blockPublicAcls(true).ignorePublicAcls(true).blockPublicPolicy(true).restrictPublicBuckets(true).build()).build());
        c.s3().putBucketEncryption(PutBucketEncryptionRequest.builder().bucket(name).serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder().rules(
                ServerSideEncryptionRule.builder().applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder().sseAlgorithm(ServerSideEncryption.AES256).build()).build()).build()).build());
        c.s3().putBucketVersioning(PutBucketVersioningRequest.builder().bucket(name).versioningConfiguration(VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build()).build());
    }

    public static List<String> buckets() {
        return List.of("bank-statements", "bank-exports", "bank-audit");
    }

    /** The fixes: close the public doors, encrypt, keep versions, narrow the policy, close the ports, require tokens, and replace what cannot be changed in place (the disks and the server). */
    public static void harden(Cloud c) {
        for (String b : List.of("bank-statements", "bank-exports")) {
            c.s3().putBucketAcl(PutBucketAclRequest.builder().bucket(b).acl(BucketCannedACL.PRIVATE).build());
            try {
                c.s3().deleteBucketPolicy(r -> r.bucket(b));
            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                // there was none
            }
            hardenBucket(c, b);
        }
        var policy = c.iam().listPolicies(r -> r.scope("Local")).policies().stream().filter(p -> p.policyName().equals("developer-everything")).findFirst().orElseThrow();
        c.iam().createPolicyVersion(r -> r.policyArn(policy.arn()).policyDocument(POLICY_READ).setAsDefault(true));
        c.iam().deletePolicyVersion(r -> r.policyArn(policy.arn()).versionId("v1"));
        for (var g : c.ec2().describeSecurityGroups().securityGroups()) {
            if (g.groupName().equals("web") || g.groupName().equals("database")) {
                for (IpPermission p : g.ipPermissions()) {
                    if (p.fromPort() == 22 || p.fromPort() == 5432) {
                        c.ec2().revokeSecurityGroupIngress(r -> r.groupId(g.groupId()).ipPermissions(p));
                    }
                }
            }
        }
        for (var r : c.ec2().describeInstances().reservations()) {
            for (var i : r.instances()) {
                c.ec2().terminateInstances(t -> t.instanceIds(i.instanceId()));
            }
        }
        for (var v : c.ec2().describeVolumes().volumes()) {
            if (!Boolean.TRUE.equals(v.encrypted())) {
                try {
                    c.ec2().deleteVolume(d -> d.volumeId(v.volumeId()));
                } catch (software.amazon.awssdk.services.ec2.model.Ec2Exception e) {
                    // still attached to the old server
                }
            }
        }
        c.ec2().runInstances(RunInstancesRequest.builder().imageId("ami-03cf127a").minCount(1).maxCount(1)
                .blockDeviceMappings(software.amazon.awssdk.services.ec2.model.BlockDeviceMapping.builder().deviceName("/dev/sda1").ebs(software.amazon.awssdk.services.ec2.model.EbsBlockDevice.builder().encrypted(true).volumeSize(8).build()).build())
                .metadataOptions(InstanceMetadataOptionsRequest.builder().httpTokens(HttpTokensState.REQUIRED).httpPutResponseHopLimit(1).build()).build());
        c.ec2().createVolume(CreateVolumeRequest.builder().availabilityZone("eu-west-1a").size(8).encrypted(true).build());
    }
}
