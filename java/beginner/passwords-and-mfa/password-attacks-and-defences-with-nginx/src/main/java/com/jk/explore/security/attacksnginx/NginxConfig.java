package com.jk.explore.security.attacksnginx;

/**
 * Writes the nginx configuration for one version of the login. The login service always answers 401, as if
 * every password were wrong, so only the rate limit decides what an attacker sees.
 */
public final class NginxConfig {

    /** What the rate limit counts attempts against. */
    public enum Key {
        /** No limit at all. */
        NONE(null),
        /** The address of the connection, which the client cannot choose. */
        ADDRESS("$binary_remote_addr"),
        /** A header the client writes itself: a mistake. */
        FORWARDED_HEADER("$http_x_forwarded_for"),
        /** The username in the request. */
        USERNAME("$arg_user");

        private final String variable;

        Key(String variable) {
            this.variable = variable;
        }

        public String variable() {
            return variable;
        }
    }

    private NginxConfig() {
    }

    public static String text(Key key, String rate, int burst) {
        StringBuilder conf = new StringBuilder();
        conf.append("events {}\n");
        conf.append("http {\n");
        if (key != Key.NONE) {
            conf.append("  limit_req_zone ").append(key.variable()).append(" zone=login:1m rate=").append(rate).append(";\n");
        }
        // The login service: a second server inside the same nginx that answers 401 to every password. It is
        // reached through proxy_pass because limit_req runs after nginx's "return" directive, so a limit placed
        // beside a bare "return" would never be applied.
        conf.append("  server {\n");
        conf.append("    listen 8081;\n");
        conf.append("    location / { return 401 \"wrong username or password\"; }\n");
        conf.append("  }\n");
        conf.append("  server {\n");
        conf.append("    listen 80;\n");
        conf.append("    location = /health { return 200 \"ok\"; }\n");
        conf.append("    location = /login {\n");
        if (key != Key.NONE) {
            conf.append("      limit_req zone=login burst=").append(burst).append(" nodelay;\n");
            conf.append("      limit_req_status 429;\n");
        }
        conf.append("      proxy_pass http://127.0.0.1:8081;\n");
        conf.append("    }\n");
        conf.append("  }\n");
        conf.append("}\n");
        return conf.toString();
    }
}
