package com.jk.explore.security.ransomres;

/** The access policies of the people and programs around the backups (S3 policy documents). */
final class Policies {

    private Policies() {
    }

    /** The backup service account as many shops run it: everything on every bucket. */
    static String everything() {
        return policy("\"s3:*\"", "\"arn:aws:s3:::*\"");
    }

    /** The backup job: it can add objects and list, nothing else (no delete, no read, no retention changes). */
    static String writerOnly(String bucket) {
        return policy("\"s3:PutObject\",\"s3:ListBucket\"", bucketAndObjects(bucket));
    }

    /** The recovery account: it can read every version, nothing else. */
    static String readerOnly(String bucket) {
        return policy("\"s3:GetObject\",\"s3:GetObjectVersion\",\"s3:ListBucket\",\"s3:ListBucketVersions\"", bucketAndObjects(bucket));
    }

    private static String bucketAndObjects(String bucket) {
        return "\"arn:aws:s3:::" + bucket + "\",\"arn:aws:s3:::" + bucket + "/*\"";
    }

    private static String policy(String actions, String resources) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[" + actions + "],\"Resource\":[" + resources + "]}]}";
    }
}
