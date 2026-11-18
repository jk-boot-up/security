package com.jk.explore.security.ransomres;

import java.util.List;

/** The access policies of the people and programs around the backups (S3 policy documents). */
final class Policies {

    /** The backup job: it can add objects and list, nothing else (no delete, no read, no retention changes). */
    static final List<String> WRITER = List.of("s3:PutObject", "s3:ListBucket");
    /** The recovery account: it can read every version, nothing else. */
    static final List<String> READER = List.of("s3:GetObject", "s3:GetObjectVersion", "s3:ListBucket", "s3:ListBucketVersions");

    private Policies() {
    }

    /** The backup service account as many shops run it: everything on every bucket. */
    static String everything() {
        return policy(List.of("s3:*"), "\"arn:aws:s3:::*\"");
    }

    static String writerOnly(String bucket) {
        return policy(WRITER, bucketAndObjects(bucket));
    }

    static String readerOnly(String bucket) {
        return policy(READER, bucketAndObjects(bucket));
    }

    private static String bucketAndObjects(String bucket) {
        return "\"arn:aws:s3:::" + bucket + "\",\"arn:aws:s3:::" + bucket + "/*\"";
    }

    private static String policy(List<String> actions, String resources) {
        String list = String.join(",", actions.stream().map(a -> "\"" + a + "\"").toList());
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[" + list + "],\"Resource\":[" + resources + "]}]}";
    }
}
