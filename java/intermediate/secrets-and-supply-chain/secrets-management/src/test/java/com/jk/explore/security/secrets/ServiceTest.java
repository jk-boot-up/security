package com.jk.explore.security.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceTest {

    private SimClock clock;
    private Database database;
    private SecretsService service;
    private final Secret shop = Secret.of("demo-shop");
    private final Secret reports = Secret.of("demo-reports");

    @BeforeEach
    void setUp() {
        clock = new SimClock();
        database = new Database(clock);
        service = new SecretsService(clock, database, 900, 3600);
        service.registerApp("shop-api", shop);
        service.registerApp("reports", reports);
    }

    @Test
    void eachApplicationGetsItsOwnLogin() throws Exception {
        SecretsService.Lease a = service.leaseDatabaseLogin("shop-api", shop);
        SecretsService.Lease b = service.leaseDatabaseLogin("reports", reports);
        assertNotEquals(a.user(), b.user());
        assertFalse(a.password().matches(b.password()));
        assertTrue(database.login(a.user(), a.password()));
        assertFalse(database.login(a.user(), b.password()));
    }

    @Test
    void aWrongApplicationCredentialIsRefusedAndAudited() {
        assertThrows(SecretsService.RefusedException.class, () -> service.leaseDatabaseLogin("shop-api", Secret.of("x")));
        assertThrows(SecretsService.RefusedException.class, () -> service.leaseDatabaseLogin("nobody", shop));
        assertTrue(service.audit().get(0).contains("refused"));
    }

    @Test
    void aLoginEndsWithItsLease() throws Exception {
        SecretsService.Lease lease = service.leaseDatabaseLogin("shop-api", shop);
        clock.advance(899);
        assertTrue(database.login(lease.user(), lease.password()));
        clock.advance(1);
        assertFalse(database.login(lease.user(), lease.password()));
        assertThrows(SecretsService.RefusedException.class, () -> service.renew(lease.id()));
        assertEquals(1, service.expireLeases());
        assertEquals(0, database.accountCount());
    }

    @Test
    void renewalsStopAtTheMaximum() throws Exception {
        SecretsService.Lease lease = service.leaseDatabaseLogin("shop-api", shop);
        clock.advance(600);
        assertEquals(1500, service.renew(lease.id()).expiresAt());
        clock.advance(800);
        assertEquals(2300, service.renew(lease.id()).expiresAt());
        clock.advance(800);
        assertEquals(3100, service.renew(lease.id()).expiresAt());
        clock.advance(800);
        assertEquals(3600, service.renew(lease.id()).expiresAt());
    }

    @Test
    void revokingEndsALoginAtOnce() throws Exception {
        SecretsService.Lease lease = service.leaseDatabaseLogin("shop-api", shop);
        service.revoke(lease.id());
        assertFalse(database.login(lease.user(), lease.password()));
        assertTrue(service.lease(lease.id()).isEmpty());
    }

    @Test
    void bothKeyVersionsWorkDuringARotationAndOnlyTheNewAfter() {
        PaymentProvider provider = new PaymentProvider();
        Secret v1 = Secret.of("demo-v1");
        Secret v2 = Secret.of("demo-v2");
        provider.issue("v1", v1);
        provider.issue("v2", v2);
        assertTrue(provider.accepts(v1) && provider.accepts(v2));
        provider.revoke("v1");
        assertFalse(provider.accepts(v1));
        assertTrue(provider.accepts(v2));
        assertFalse(provider.accepts(Secret.of("demo-v3")));
    }
}
