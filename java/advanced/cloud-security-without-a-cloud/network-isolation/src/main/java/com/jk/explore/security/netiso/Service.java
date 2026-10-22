package com.jk.explore.security.netiso;

/** One service of the shop: a name, the port it listens on and what it is. */
public record Service(String name, int port, String role) {

    /** The shop: a web front, an application, a database, a cache, an admin panel and a build server. */
    public static java.util.List<Service> shop() {
        return java.util.List.of(new Service("web", 8000, "the public front"), new Service("app", 8080, "the application"), new Service("db", 5432, "the accounts database"), new Service("cache", 6379, "the session cache"),
                new Service("admin", 9000, "the admin panel"), new Service("ci", 2222, "the build server"));
    }
}
