package com.jk.explore.security.misconfig;

/** Checks run before the store opens its port. A production store with a default secret or a debug switch does not start at all. */
final class StartupGate {

    private StartupGate() {
    }

    static void check(StoreConfig config) throws InsecureConfigException {
        if (!"prod".equals(config.profile())) {
            return;
        }
        String weakness = Secrets.weakness(config.adminPassword());
        if (!weakness.isEmpty()) {
            throw new InsecureConfigException("admin password " + weakness);
        }
        if (config.debugEndpoint() || config.detailedErrors() || config.traceAllowed()) {
            throw new InsecureConfigException("a debugging switch is on in production");
        }
        if (!config.testSettings().isEmpty()) {
            throw new InsecureConfigException("test settings are present in production: " + String.join(", ", config.testSettings()));
        }
    }
}
