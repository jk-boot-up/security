package com.jk.explore.security.misconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GateAndDriftTest {

    @TempDir
    Path dir;

    @ParameterizedTest
    @ValueSource(strings = {"", "admin", "ADMIN", "changeme", "short-pw"})
    void defaultOrShortPasswordsAreRefusedInProduction(String password) {
        Properties p = Profiles.hardened();
        p.setProperty(StoreConfig.ADMIN_PASSWORD, password);
        assertThrows(InsecureConfigException.class, () -> StartupGate.check(StoreConfig.from(p)));
    }

    @Test
    void debuggingSwitchesAndTestSettingsAreRefusedInProduction() {
        assertThrows(InsecureConfigException.class, () -> StartupGate.check(StoreConfig.from(Profiles.copiedFromDevelopment())));
        Properties p = Profiles.hardened();
        p.setProperty("test.fake-payments", "true");
        assertThrows(InsecureConfigException.class, () -> StartupGate.check(StoreConfig.from(p)));
    }

    @Test
    void theHardenedProfilePassesAndDevelopmentIsNotGated() {
        assertDoesNotThrow(() -> StartupGate.check(StoreConfig.from(Profiles.hardened())));
        assertDoesNotThrow(() -> StartupGate.check(StoreConfig.from(Profiles.shipped())));
    }

    @Test
    void driftFindsDevelopmentValuesDevelopmentOnlySettingsAndUnreviewedOnes() {
        ProfileDrift.Report report = ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), Profiles.copiedFromDevelopment());
        assertEquals(Set.of(StoreConfig.DEBUG_ENDPOINT, StoreConfig.ERRORS_DETAILED), report.stillDevelopment());
        assertEquals(Set.of("test.fake-payments"), report.developmentOnly());
        assertTrue(ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), Profiles.hardened()).clean());
        Properties extra = Profiles.hardened();
        extra.setProperty("uploads.max-size", "unlimited");
        assertEquals(Set.of("uploads.max-size"), ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), extra).unreviewed());
    }

    @Test
    void settingsReadCaseInsensitivelyAndMissingFlagsAreOff() {
        Properties p = new Properties();
        p.setProperty(StoreConfig.PROFILE, " PROD ");
        p.setProperty(StoreConfig.DEBUG_ENDPOINT, "TRUE");
        StoreConfig config = StoreConfig.from(p);
        assertEquals("prod", config.profile());
        assertTrue(config.debugEndpoint());
        assertFalse(config.traceAllowed());
    }

    @Test
    void passwordComparisonIsExact() {
        assertTrue(Secrets.same("abc", "abc"));
        assertFalse(Secrets.same("abc", "abd"));
        assertFalse(Secrets.same("abc", "abcd"));
    }

    @Test
    void filePermissionsAreReported() throws Exception {
        Path loose = dir.resolve("loose");
        ConfigFile.write(loose, Profiles.hardened(), "rw-r-----");
        assertTrue(ConfigFile.readableByOthers(loose));
        Path tight = dir.resolve("tight");
        ConfigFile.write(tight, Profiles.hardened(), "rw-------");
        assertFalse(ConfigFile.readableByOthers(tight));
        assertEquals("rw-------", ConfigFile.permissions(tight));
    }
}
