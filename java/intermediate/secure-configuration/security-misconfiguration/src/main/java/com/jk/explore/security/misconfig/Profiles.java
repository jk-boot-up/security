package com.jk.explore.security.misconfig;

import java.util.Properties;

/** The settings files the demo uses: what the store ships with, a hardened production profile, and a production profile copied from development. */
final class Profiles {

    /** A demo value standing in for a long random secret injected at deploy time; it is never printed. */
    static final String DEMO_PRODUCTION_SECRET = "demo-only-7Hq2-long-random-production-secret";

    private Profiles() {
    }

    /** What the store ships with: comfortable for a developer on a laptop. */
    static Properties shipped() {
        Properties p = new Properties();
        p.setProperty(StoreConfig.PROFILE, "dev");
        p.setProperty(StoreConfig.ADMIN_PASSWORD, "admin");
        p.setProperty(StoreConfig.DEBUG_ENDPOINT, "true");
        p.setProperty(StoreConfig.STATIC_LISTING, "true");
        p.setProperty(StoreConfig.ERRORS_DETAILED, "true");
        p.setProperty(StoreConfig.SERVER_BANNER, "ShopServer/1.4.2 (Java 25)");
        p.setProperty(StoreConfig.HTTP_TRACE, "true");
        p.setProperty("test.fake-payments", "true");
        return p;
    }

    /** The production profile: every switch stated, every risky one off. */
    static Properties hardened() {
        Properties p = new Properties();
        p.setProperty(StoreConfig.PROFILE, "prod");
        p.setProperty(StoreConfig.ADMIN_PASSWORD, DEMO_PRODUCTION_SECRET);
        p.setProperty(StoreConfig.DEBUG_ENDPOINT, "false");
        p.setProperty(StoreConfig.STATIC_LISTING, "false");
        p.setProperty(StoreConfig.ERRORS_DETAILED, "false");
        p.setProperty(StoreConfig.SERVER_BANNER, "");
        p.setProperty(StoreConfig.HTTP_TRACE, "false");
        return p;
    }

    /** Production made by copying the development file and changing what someone remembered to change. */
    static Properties copiedFromDevelopment() {
        Properties p = shipped();
        p.setProperty(StoreConfig.PROFILE, "prod");
        p.setProperty(StoreConfig.ADMIN_PASSWORD, DEMO_PRODUCTION_SECRET);
        p.setProperty(StoreConfig.STATIC_LISTING, "false");
        p.setProperty(StoreConfig.SERVER_BANNER, "");
        p.setProperty(StoreConfig.HTTP_TRACE, "false");
        return p;
    }
}
