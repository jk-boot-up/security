package com.jk.explore.security.cloudmisconf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImdsTest {

    @Test
    void version1AnswersAPlainGet() throws Exception {
        try (Imds imds = new Imds(false)) {
            Imds.Fetched f = Imds.fetchUrl(imds.base() + Imds.ROLE_PATH);
            assertEquals(200, f.status());
            assertTrue(f.leaked());
        }
    }

    @Test
    void version2RefusesAPlainGet() throws Exception {
        try (Imds imds = new Imds(true)) {
            Imds.Fetched f = Imds.fetchUrl(imds.base() + Imds.ROLE_PATH);
            assertEquals(401, f.status());
            assertFalse(f.leaked());
        }
    }

    @Test
    void version2AnswersWhenTheCallerCanPutAndSetAHeader() throws Exception {
        try (Imds imds = new Imds(true)) {
            Imds.Fetched token = Imds.webhookTester("PUT", imds.base() + Imds.TOKEN_PATH, Imds.TTL_HEADER, "60");
            Imds.Fetched f = Imds.webhookTester("GET", imds.base() + Imds.ROLE_PATH, "X-aws-ec2-metadata-token", token.body());
            assertTrue(f.leaked());
        }
    }

    @Test
    void aTokenRequestThatPassedThroughAProxyIsRefused() throws Exception {
        try (Imds imds = new Imds(true)) {
            assertEquals(403, Imds.throughProxy("PUT", imds.base() + Imds.TOKEN_PATH, Imds.TTL_HEADER, "60").status());
        }
    }

    @Test
    void version1StillAnswersThroughAProxy() throws Exception {
        try (Imds imds = new Imds(false)) {
            assertTrue(Imds.throughProxy("GET", imds.base() + Imds.ROLE_PATH, null, null).leaked());
        }
    }

    @Test
    void aForgedTokenIsRefused() throws Exception {
        try (Imds imds = new Imds(true)) {
            assertEquals(401, Imds.webhookTester("GET", imds.base() + Imds.ROLE_PATH, "X-aws-ec2-metadata-token", "made-up").status());
        }
    }
}
