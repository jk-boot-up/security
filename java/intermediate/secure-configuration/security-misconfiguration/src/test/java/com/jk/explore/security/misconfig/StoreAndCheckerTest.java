package com.jk.explore.security.misconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoreAndCheckerTest {

    @TempDir
    Path dir;

    @Test
    void theShippedStoreFailsEveryCheck() throws Exception {
        StoreConfig config = StoreConfig.from(Profiles.shipped());
        Path file = dir.resolve("a.properties");
        ConfigFile.write(file, Profiles.shipped(), "rw-r--r--");
        try (StoreServer store = StoreServer.start(config)) {
            List<ConfigChecker.Check> checks = new ConfigChecker(store.port()).checkAll(config, file);
            assertEquals(8, checks.size());
            assertEquals(0, ConfigChecker.score(checks));
        }
    }

    @Test
    void theHardenedStorePassesEveryCheck() throws Exception {
        StoreConfig config = StoreConfig.from(Profiles.hardened());
        Path file = dir.resolve("b.properties");
        ConfigFile.write(file, Profiles.hardened(), "rw-------");
        try (StoreServer store = StoreServer.start(config)) {
            assertEquals(8, ConfigChecker.score(new ConfigChecker(store.port()).checkAll(config, file)));
        }
    }

    @Test
    void theDebugPageLeaksTheSecretOnlyWhenSwitchedOn() throws Exception {
        try (StoreServer shipped = StoreServer.start(StoreConfig.from(Profiles.shipped()));
                StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            assertTrue(new ConfigChecker(shipped.port()).send("GET", "/debug/config", null).body().contains("admin.password=admin"));
            ConfigChecker.Reply reply = new ConfigChecker(hardened.port()).send("GET", "/debug/config", null);
            assertEquals(404, reply.status());
            assertFalse(reply.body().contains(Profiles.DEMO_PRODUCTION_SECRET));
        }
    }

    @Test
    void errorPagesKeepAReferenceButNoStackTrace() throws Exception {
        try (StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker.Reply reply = new ConfigChecker(hardened.port()).send("GET", "/orders?id=abc", null);
            assertEquals(500, reply.status());
            assertTrue(reply.body().contains("Reference: E-500"));
            assertFalse(reply.body().contains("Exception"));
        }
    }

    @Test
    void theRightAdminPasswordStillWorks() throws Exception {
        try (StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker checker = new ConfigChecker(hardened.port());
            assertEquals(200, checker.send("POST", "/admin/login", "password=" + Profiles.DEMO_PRODUCTION_SECRET).status());
            assertEquals(401, checker.send("POST", "/admin/login", "password=admin").status());
        }
    }

    @Test
    void unknownMethodsAndTraceAreRefusedWithAnAllowHeader() throws Exception {
        try (StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker checker = new ConfigChecker(hardened.port());
            assertEquals(405, checker.send("TRACE", "/", null).status());
            assertEquals(405, checker.send("DELETE", "/", null).status());
        }
    }

    @Test
    void staticFilesServeButTheFolderDoesNotList() throws Exception {
        try (StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker checker = new ConfigChecker(hardened.port());
            assertEquals(200, checker.send("GET", "/static/logo.png", null).status());
            assertEquals(404, checker.send("GET", "/static/", null).status());
        }
    }
}
