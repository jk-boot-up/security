package com.jk.explore.security.attacksnginx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NginxConfigTest {

    @Test
    void noLimitMeansNoZoneAndNoLimitReq() {
        String conf = NginxConfig.text(NginxConfig.Key.NONE, "1r/m", 5);
        assertFalse(conf.contains("limit_req"));
        assertTrue(conf.contains("proxy_pass http://127.0.0.1:8081;"));
    }

    @Test
    void theZoneIsKeyedOnTheChosenVariable() {
        assertTrue(NginxConfig.text(NginxConfig.Key.ADDRESS, "1r/m", 5).contains("limit_req_zone $binary_remote_addr zone=login:1m rate=1r/m;"));
        assertTrue(NginxConfig.text(NginxConfig.Key.FORWARDED_HEADER, "1r/m", 5).contains("limit_req_zone $http_x_forwarded_for "));
        assertTrue(NginxConfig.text(NginxConfig.Key.USERNAME, "1r/m", 5).contains("limit_req_zone $arg_user "));
    }

    @Test
    void theLimitUsesTheBurstWithNodelayAndAnswers429() {
        String conf = NginxConfig.text(NginxConfig.Key.ADDRESS, "1r/m", 5);
        assertTrue(conf.contains("limit_req zone=login burst=5 nodelay;"));
        assertTrue(conf.contains("limit_req_status 429;"));
    }

    @Test
    void theLimitIsAppliedToTheProxiedLoginNotToABareReturn() {
        String conf = NginxConfig.text(NginxConfig.Key.ADDRESS, "1r/m", 5);
        int limit = conf.indexOf("limit_req zone");
        int proxy = conf.indexOf("proxy_pass");
        assertTrue(limit > 0 && proxy > limit, "limit_req must sit in the same location as proxy_pass");
        assertFalse(conf.substring(conf.indexOf("location = /login")).contains("return 401"));
    }
}
