package com.jk.explore.security.attacksnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests start real nginx containers, so they need a running Docker: run them with ./gradlew integrationTest. */
@Tag("docker")
class NginxContainerTest {

    private Tally tally(NginxConfig.Key key, java.util.function.IntFunction<String> user,
                        java.util.function.IntFunction<String> forwardedFor) throws IOException, InterruptedException {
        try (Nginx nginx = Nginx.start(NginxConfig.text(key, "1r/m", 5))) {
            return new Attacker(nginx.port()).run(20, user, forwardedFor);
        }
    }

    @Test
    void withoutALimitEveryGuessIsAnswered() throws Exception {
        assertEquals(new Tally(20, 0), tally(NginxConfig.Key.NONE, i -> "dana", i -> null));
    }

    @Test
    void aLimitOnTheAddressAllowsOnePlusTheBurst() throws Exception {
        assertEquals(new Tally(6, 14), tally(NginxConfig.Key.ADDRESS, i -> "dana", i -> null));
    }

    @Test
    void aLimitKeyedOnAHeaderTheClientWritesIsBypassed() throws Exception {
        assertEquals(new Tally(20, 0), tally(NginxConfig.Key.FORWARDED_HEADER, i -> "dana", i -> "10.0.0." + i));
    }

    @Test
    void aLimitOnTheRealAddressIgnoresTheHeader() throws Exception {
        assertEquals(new Tally(6, 14), tally(NginxConfig.Key.ADDRESS, i -> "dana", i -> "10.0.0." + i));
    }

    @Test
    void aLimitPerUsernameStopsTheOwnerToo() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.USERNAME, "1r/m", 5))) {
            Attacker attacker = new Attacker(nginx.port());
            attacker.run(20, i -> "dana", i -> null);
            assertEquals(429, attacker.attempt("dana", "203.0.113.7"));
            assertEquals(401, attacker.attempt("erin", null));
        }
    }

    @Test
    void aSprayAcrossUsernamesNeverReachesAPerUsernameLimit() throws Exception {
        assertEquals(new Tally(20, 0), tally(NginxConfig.Key.USERNAME, i -> "customer" + i, i -> null));
    }

    @Test
    void closingTheContainerStopsIt() throws Exception {
        Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.NONE, "1r/m", 5));
        int port = nginx.port();
        nginx.close();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new Attacker(port).attempt("dana", null));
    }

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), NginxAttacksDemo.run());
    }
}
