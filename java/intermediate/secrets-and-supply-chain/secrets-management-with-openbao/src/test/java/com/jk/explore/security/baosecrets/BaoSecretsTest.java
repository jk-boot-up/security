package com.jk.explore.security.baosecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real OpenBao and a real PostgreSQL in Docker. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaoSecretsTest {

    private BaoLab lab;
    private Bao root;

    @BeforeAll
    void start() throws Exception {
        lab = BaoLab.start();
        root = new Bao(lab.baoUrl(), BaoLab.ROOT_TOKEN);
        BaoSecretsDemo.setUp(lab, root);
    }

    @AfterAll
    void stop() {
        lab.close();
    }

    private boolean connects(String user, String password) {
        try (Connection ignored = DriverManager.getConnection(lab.jdbcUrl(), user, password)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Test
    void aSecretIdWorksOnce() throws Exception {
        String id = BaoSecretsDemo.newSecretId(root, "shop-api");
        assertEquals(200, BaoSecretsDemo.loginReply(root, "shop-api", id).status());
        assertEquals(400, BaoSecretsDemo.loginReply(root, "shop-api", id).status());
    }

    @Test
    void aSecretIdOfAnotherRoleIsRefused() throws Exception {
        assertEquals(400, BaoSecretsDemo.loginReply(root, "reports", BaoSecretsDemo.newSecretId(root, "shop-api")).status());
    }

    @Test
    void aTokenCarriesOnlyItsApplicationsPolicy() throws Exception {
        Bao shop = BaoSecretsDemo.login(root, "shop-api");
        assertEquals(200, shop.get("secret/data/shop-api/payment").status());
        assertEquals(403, shop.get("secret/data/reports/warehouse").status());
        assertEquals(403, shop.get("database/creds/reports").status());
        assertEquals(403, shop.put("sys/policies/acl/shop-api", Map.of("policy", "path \"*\" { capabilities = [\"sudo\"] }")).status());
        assertEquals(403, root.withToken(null).get("secret/data/shop-api/payment").status());
    }

    @Test
    void versionsAreReadableByNumberUntilDestroyed() throws Exception {
        root.post("secret/data/shop-api/versions", Map.of("data", Map.of("k", "one")));
        root.post("secret/data/shop-api/versions", Map.of("data", Map.of("k", "two")));
        assertEquals("one", ((Map<?, ?>) root.get("secret/data/shop-api/versions?version=1").data().get("data")).get("k"));
        assertEquals("two", ((Map<?, ?>) root.get("secret/data/shop-api/versions").data().get("data")).get("k"));
        root.put("secret/destroy/shop-api/versions", Map.of("versions", List.of(1)));
        assertEquals(404, root.get("secret/data/shop-api/versions?version=1").status());
        assertEquals(200, root.get("secret/data/shop-api/versions").status());
    }

    @Test
    void theAuditTrailHashesValues() throws Exception {
        root.post("secret/data/shop-api/audited", Map.of("data", Map.of("k", "very-secret-value-123")));
        String audit = lab.auditLog();
        assertTrue(audit.contains("secret/data/shop-api/audited"));
        assertFalse(audit.contains("very-secret-value-123"));
        assertTrue(audit.contains("hmac-sha256:"));
    }

    @Test
    void eachApplicationGetsItsOwnLoginWithOnlyItsGrants() throws Exception {
        Bao.Reply a = BaoSecretsDemo.login(root, "shop-api").get("database/creds/shop-api");
        Bao.Reply b = BaoSecretsDemo.login(root, "reports").get("database/creds/reports");
        assertNotEquals(a.data().get("username"), b.data().get("username"));
        assertTrue(connects(String.valueOf(a.data().get("username")), String.valueOf(a.data().get("password"))));
        assertEquals(8, ((Number) a.json().get("lease_duration")).intValue());
    }

    @Test
    void revokingALeaseEndsTheLoginAtOnce() throws Exception {
        Bao.Reply a = BaoSecretsDemo.login(root, "shop-api").get("database/creds/shop-api");
        String user = String.valueOf(a.data().get("username"));
        String password = String.valueOf(a.data().get("password"));
        assertTrue(connects(user, password));
        assertEquals(204, root.put("sys/leases/revoke", Map.of("lease_id", a.json().get("lease_id"))).status());
        assertFalse(connects(user, password));
    }

    @Test
    void anUnrenewedLeaseEndsOnItsOwn() throws Exception {
        Bao.Reply a = BaoSecretsDemo.login(root, "reports").get("database/creds/reports");
        Thread.sleep(9_500);
        assertFalse(connects(String.valueOf(a.data().get("username")), String.valueOf(a.data().get("password"))));
    }

    @Test
    void aRenewalIsCappedAtTheMaximum() throws Exception {
        Bao.Reply a = BaoSecretsDemo.login(root, "shop-api").get("database/creds/shop-api");
        Bao.Reply renewed = root.put("sys/leases/renew", Map.of("lease_id", a.json().get("lease_id"), "increment", 600));
        assertTrue(((Number) renewed.json().get("lease_duration")).intValue() <= 20);
    }

    @Test
    void revokingAPrefixEndsEveryLoginOfOneApplicationOnly() throws Exception {
        Bao shop = BaoSecretsDemo.login(root, "shop-api");
        Bao.Reply s1 = shop.get("database/creds/shop-api");
        Bao.Reply s2 = shop.get("database/creds/shop-api");
        Bao.Reply r = BaoSecretsDemo.login(root, "reports").get("database/creds/reports");
        root.put("sys/leases/revoke-prefix/database/creds/shop-api", Map.of());
        assertFalse(connects(String.valueOf(s1.data().get("username")), String.valueOf(s1.data().get("password"))));
        assertFalse(connects(String.valueOf(s2.data().get("username")), String.valueOf(s2.data().get("password"))));
        assertTrue(connects(String.valueOf(r.data().get("username")), String.valueOf(r.data().get("password"))));
    }

    @Test
    void rotatingAStaticRoleEndsTheOldPassword() throws Exception {
        String before = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        root.post("database/rotate-role/payments", Map.of());
        String after = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        assertNotEquals(before, after);
        assertFalse(connects("payments", before));
        assertTrue(connects("payments", after));
    }

    @Test
    void aSealedServerRefusesEveryRequestAndRecoversWhenUnsealed() throws Exception {
        Bao shop = BaoSecretsDemo.login(root, "shop-api");
        root.put("sys/seal", Map.of());
        try {
            assertEquals(503, shop.get("secret/data/shop-api/payment").status());
            assertEquals(503, shop.get("database/creds/shop-api").status());
        } finally {
            root.withToken(null).put("sys/unseal", Map.of("key", lab.unsealKey()));
        }
        assertEquals(200, shop.get("secret/data/shop-api/payment").status());
    }
}
