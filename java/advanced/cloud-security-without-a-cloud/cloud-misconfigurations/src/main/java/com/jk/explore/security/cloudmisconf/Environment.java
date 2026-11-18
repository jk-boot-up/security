package com.jk.explore.security.cloudmisconf;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
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

    static final String IMAGE = "ami-03cf127a";
    static final TagSpecification NAME_WEB = TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(Tag.builder().key("Name").value("web").build()).build();

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
        c.ec2().runInstances(RunInstancesRequest.builder().imageId(IMAGE).minCount(1).maxCount(1).securityGroupIds(web).tagSpecifications(NAME_WEB)
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

    /** One change made by the fixes: "edit" changes a resource in place, "replace" deletes it and creates a new one. */
    public record Fix(String kind, String what) {
        @Override
        public String toString() {
            return String.format("%-8s %s", kind, what);
        }
    }

    /**
     * The fixes: close the public doors, encrypt, keep versions, narrow the policy, close the ports and require tokens, all
     * in place; then replace what cannot be changed in place, because a disk cannot be encrypted after it was made.
     */
    public static List<Fix> harden(Cloud c) {
        List<Fix> done = new ArrayList<>();
        c.s3().putBucketAcl(PutBucketAclRequest.builder().bucket("bank-statements").acl(BucketCannedACL.PRIVATE).build());
        done.add(new Fix("edit", "ACL private on bank-statements"));
        c.s3().deleteBucketPolicy(r -> r.bucket("bank-exports"));
        done.add(new Fix("edit", "bucket policy deleted on bank-exports"));
        hardenBucket(c, "bank-statements");
        hardenBucket(c, "bank-exports");
        done.add(new Fix("edit", "block all, AES256, versions on 2 buckets"));
        var policy = c.iam().listPolicies(r -> r.scope("Local")).policies().stream().filter(p -> p.policyName().equals("developer-everything")).findFirst().orElseThrow();
        c.iam().createPolicyVersion(r -> r.policyArn(policy.arn()).policyDocument(POLICY_READ).setAsDefault(true));
        c.iam().deletePolicyVersion(r -> r.policyArn(policy.arn()).versionId("v1"));
        done.add(new Fix("edit", "developer-everything: s3:GetObject only"));
        for (var g : c.ec2().describeSecurityGroups().securityGroups()) {
            for (IpPermission p : g.ipPermissions()) {
                if (p.fromPort() != null && (p.fromPort() == 22 || p.fromPort() == 5432)) {
                    c.ec2().revokeSecurityGroupIngress(r -> r.groupId(g.groupId()).ipPermissions(p));
                    done.add(new Fix("edit", "port " + p.fromPort() + " closed on group " + g.groupName()));
                }
            }
        }
        Instance old = c.ec2().describeInstances().reservations().getFirst().instances().getFirst();
        c.ec2().modifyInstanceMetadataOptions(r -> r.instanceId(old.instanceId())
                .httpTokens(HttpTokensState.REQUIRED).httpPutResponseHopLimit(1));
        done.add(new Fix("edit", "tokens required, hop limit 1 on server web"));
        c.ec2().terminateInstances(t -> t.instanceIds(old.instanceId()));
        c.ec2().runInstances(RunInstancesRequest.builder().imageId(IMAGE).minCount(1).maxCount(1).tagSpecifications(NAME_WEB)
                .blockDeviceMappings(BlockDeviceMapping.builder().deviceName("/dev/sda1").ebs(EbsBlockDevice.builder().encrypted(true).volumeSize(8).build()).build())
                .metadataOptions(InstanceMetadataOptionsRequest.builder().httpTokens(HttpTokensState.REQUIRED).httpPutResponseHopLimit(1).build()).build());
        done.add(new Fix("replace", "server web: new, with an encrypted root disk"));
        for (var v : c.ec2().describeVolumes().volumes()) {
            if (!Boolean.TRUE.equals(v.encrypted()) && v.attachments().isEmpty()) {
                c.ec2().deleteVolume(d -> d.volumeId(v.volumeId()));
            }
        }
        c.ec2().createVolume(CreateVolumeRequest.builder().availabilityZone("eu-west-1a").size(8).encrypted(true).build());
        done.add(new Fix("replace", "data disk: new, encrypted"));
        return done;
    }

    /** What the emulator answers to a public policy and a public ACL on a bucket whose public access is blocked. */
    public static List<String> tryToOpen(Cloud c, String bucket) {
        String policy = PUBLIC_BUCKET_POLICY.replace("bank-exports", bucket);
        int byPolicy = c.s3().putBucketPolicy(r -> r.bucket(bucket).policy(policy)).sdkHttpResponse().statusCode();
        int byAcl = c.s3().putBucketAcl(r -> r.bucket(bucket).acl(BucketCannedACL.PUBLIC_READ)).sdkHttpResponse().statusCode();
        return List.of(String.format("%-16s %d accepted", "PutBucketPolicy", byPolicy), String.format("%-16s %d accepted", "PutBucketAcl", byAcl));
    }
}
