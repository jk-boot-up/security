package com.jk.explore.security.backupsrustfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Tests that need no Docker: the checksum helper, the outcome wording and the write-only policy. */
class StoreUnitTest {

    @Test
    void sha256MatchesThePublishedValueForAbc() {
        // FIPS 180-4 example: SHA-256("abc") = ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad
        byte[] expected = java.util.HexFormat.of().parseHex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertEquals(java.util.Base64.getEncoder().encodeToString(expected), Store.sha256Base64(Store.bytes("abc")));
    }

    @Test
    void differentBytesGiveDifferentChecksums() {
        assertFalse(Store.sha256Base64(Store.bytes("a")).equals(Store.sha256Base64(Store.bytes("b"))));
    }

    @Test
    void outcomeNamesTheStatusAndTheCodeOfARefusal() {
        String text = Store.outcome(() -> {
            throw (S3Exception) S3Exception.builder().statusCode(403)
                    .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build()).build();
        });
        assertEquals("refused (403 AccessDenied)", text);
    }

    @Test
    void outcomeSaysAllowedWhenNothingIsThrown() {
        assertEquals("allowed", Store.outcome(() -> { }));
    }

    @Test
    void theWriteOnlyPolicyHasExactlyOneActionOnOneBucket() {
        String policy = ObjectStoreContainer.writeOnlyPolicy("shop-backups");
        assertTrue(policy.contains("\"Action\":[\"s3:PutObject\"]"));
        assertTrue(policy.contains("arn:aws:s3:::shop-backups/*"));
        assertEquals(3, policy.split("s3:", -1).length, "one s3: in the action name and one in the arn prefix");
        assertFalse(policy.contains("s3:*"));
        assertFalse(policy.contains("Delete"));
        assertFalse(policy.contains("GetObject"));
    }
}
