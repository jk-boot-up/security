package com.jk.explore.security.baosecrets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/** Runs the store's secrets story in five numbered acts against a real OpenBao and a real PostgreSQL in Docker, all on the loopback interface. */
public class BaoSecretsDemo {

    static final String CREATE = "CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT ON orders TO \"{{name}}\";";
    static final String RENEW = "ALTER ROLE \"{{name}}\" VALID UNTIL '{{expiration}}';";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Tokens, passwords, lease ids and ports are never printed. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (BaoLab lab = BaoLab.start()) {
            Bao root = new Bao(lab.baoUrl(), BaoLab.ROOT_TOKEN);
            setUp(lab, root);
            act1WhoMayAsk(out, root);
            act2StaticVersions(out, lab, root);
            act3LeasedLogins(out, lab, root);
            act4RotationAndBlastRadius(out, lab, root);
            act5Bill(out, lab, root);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The store's data, the secrets service's policies, and one database login per application. */
    static void setUp(BaoLab lab, Bao root) throws Exception {
        try (Connection admin = DriverManager.getConnection(lab.jdbcUrl(), "postgres", BaoLab.DB_ADMIN_PASSWORD); Statement s = admin.createStatement()) {
            s.execute("CREATE TABLE orders (id int, item text)");
            s.execute("INSERT INTO orders VALUES (1, 'kettle'), (2, 'toaster')");
            s.execute("CREATE TABLE card_numbers (id int, pan text)");
            s.execute("INSERT INTO card_numbers VALUES (1, '4111111111111111')");
            s.execute("CREATE ROLE payments LOGIN PASSWORD 'initial-password'");
        }
        root.post("sys/auth/approle", Map.of("type", "approle"));
        root.post("sys/mounts/database", Map.of("type", "database"));
        root.post("database/config/shopdb", Map.of("plugin_name", "postgresql-database-plugin", "allowed_roles", "*",
                "connection_url", "postgresql://{{username}}:{{password}}@db:5432/postgres?sslmode=disable", "username", "postgres", "password", BaoLab.DB_ADMIN_PASSWORD));
        for (String app : List.of("shop-api", "reports")) {
            root.put("sys/policies/acl/" + app, Map.of("policy", policy(app)));
            root.post("auth/approle/role/" + app, Map.of("token_policies", app, "token_ttl", "120s",
                    "secret_id_num_uses", 1, "secret_id_ttl", "60s"));
            root.post("database/roles/" + app, Map.of("db_name", "shopdb",
                    "creation_statements", CREATE, "renew_statements", RENEW,
                    "default_ttl", "8s", "max_ttl", "20s"));
        }
        root.post("secret/data/shop-api/payment", Map.of("data", Map.of("api-key", "key-v1")));
        root.post("secret/data/reports/warehouse", Map.of("data", Map.of("api-key", "warehouse-key")));
        root.post("database/static-roles/payments", Map.of("db_name", "shopdb", "username", "payments", "rotation_period", "24h"));
    }

    /** What one application may do: read its own secrets and lease its own database logins, nothing else. */
    static String policy(String app) {
        return "path \"secret/data/" + app + "/*\" { capabilities = [\"read\"] }\n"
                + "path \"database/creds/" + app + "\" { capabilities = [\"read\"] }";
    }

    /** An application proves who it is with its role id and a secret id that works once, and gets a token that carries only its policy. */
    static Bao login(Bao root, String app) throws IOException, InterruptedException {
        Bao.Reply r = loginReply(root, app, newSecretId(root, app));
        return root.withToken(String.valueOf(r.auth().get("client_token")));
    }

    static String newSecretId(Bao root, String app) throws IOException, InterruptedException {
        return String.valueOf(root.post("auth/approle/role/" + app + "/secret-id", Map.of()).data().get("secret_id"));
    }

    static Bao.Reply loginReply(Bao root, String app, String secretId) throws IOException, InterruptedException {
        String roleId = String.valueOf(root.get("auth/approle/role/" + app + "/role-id").data().get("role_id"));
        return root.withToken(null).post("auth/approle/login", Map.of("role_id", roleId, "secret_id", secretId));
    }

    private static String outcome(int status) {
        return (status == 200 || status == 204 ? "allowed" : "refused") + " (" + status + ")";
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
    }

    private static void act1WhoMayAsk(StringBuilder out, Bao root) throws Exception {
        line(out, "ONE. A secrets server, and who may ask.");
        line(out, "  THE POLICY FOR shop-api (reports has the same, with its name):");
        policy("shop-api").lines().forEach(l -> line(out, "    " + l));
        String secretId = newSecretId(root, "shop-api");
        line(out, "  THE REQUEST: POST auth/approle/login");
        line(out, "    role_id    shop-api's role id (not shown)");
        line(out, "    secret_id  a fresh one that works once (not shown)");
        Bao.Reply first = loginReply(root, "shop-api", secretId);
        line(out, "  THE RESPONSE:");
        line(out, "    status     " + outcome(first.status()));
        line(out, "    token      policies " + first.auth().get("policies") + ", " + first.auth().get("lease_duration") + " seconds");
        line(out, "  THE SAME SECRET ID AGAIN, AS A THIEF WHO SAW IT WOULD:");
        line(out, "    status     " + outcome(loginReply(root, "shop-api", secretId).status()));
        line(out, "  A SECRET ID NOBODY ISSUED:");
        line(out, "    status     " + outcome(loginReply(root, "shop-api", "00000000-0000-0000-0000-000000000000").status()));
        Bao shop = root.withToken(String.valueOf(first.auth().get("client_token")));
        Bao reports = login(root, "reports");
        line(out, "  HOW THE POLICY DECIDES A READ:");
        line(out, "    " + pad("token", 10) + pad("path", 31) + pad("rule", 9) + "OpenBao");
        String[][] asks = {
            {"shop-api", "secret/data/shop-api/payment"}, {"shop-api", "secret/data/reports/warehouse"},
            {"shop-api", "database/creds/reports"}, {"reports", "secret/data/reports/warehouse"},
            {"reports", "secret/data/shop-api/payment"}, {"none", "secret/data/shop-api/payment"}};
        for (String[] ask : asks) {
            Bao who = switch (ask[0]) {
                case "shop-api" -> shop;
                case "reports" -> reports;
                default -> root.withToken(null);
            };
            List<PolicyRule.Rule> rules = ask[0].equals("none") ? List.of() : PolicyRule.parse(policy(ask[0]));
            line(out, "    " + pad(ask[0], 10) + pad(ask[1], 31) + pad(PolicyRule.allows(rules, ask[1], "read") ? "allowed" : "refused", 9)
                    + outcome(who.get(ask[1]).status()));
        }
    }

    private static void act2StaticVersions(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "TWO. Secrets that stay still: versions, rotation and the audit trail.");
        Bao shop = login(root, "shop-api");
        String path = "secret/data/shop-api/payment";
        line(out, "  BEFORE THE ROTATION, WHAT THE STORE READS:");
        line(out, "    current    " + shop.get(path).data().get("data"));
        root.post(path, Map.of("data", Map.of("api-key", "key-v2")));
        line(out, "  AFTER THE PROVIDER ISSUES v2 (written as version 2):");
        line(out, "    version 1  " + shop.get(path + "?version=1").data().get("data"));
        line(out, "    version 2  " + shop.get(path + "?version=2").data().get("data"));
        line(out, "    current    " + shop.get(path).data().get("data"));
        line(out, "  DESTROYING VERSION 1, ONCE EVERY SERVER HAS SWITCHED:");
        line(out, "    by the application  " + outcome(shop.put("secret/destroy/shop-api/payment", Map.of("versions", List.of(1))).status()));
        line(out, "    by the operator     " + outcome(root.put("secret/destroy/shop-api/payment", Map.of("versions", List.of(1))).status()));
        line(out, "    then version 1      " + outcome(shop.get(path + "?version=1").status()));
        line(out, "    then version 2      " + outcome(shop.get(path + "?version=2").status()));
        String audit = lab.auditLog();
        line(out, "  THE AUDIT TRAIL:");
        line(out, "    " + pad("requests for " + path, 43) + audit.contains(path));
        line(out, "    " + pad("the application that asked, shop-api", 43) + audit.contains("shop-api"));
        line(out, "    " + pad("the key itself, key-v1 or key-v2", 43) + (audit.contains("key-v1") || audit.contains("key-v2")));
        Bao.Reply hashed = root.post("sys/audit-hash/to_file", Map.of("input", "key-v2"));
        String hash = String.valueOf(hashed.data() != null && hashed.data().get("hash") != null ? hashed.data().get("hash") : hashed.json().get("hash"));
        String prefix = "hmac-sha256:";
        line(out, "  HOW THE AUDIT TRAIL HIDES A VALUE (HMAC-SHA256, server's key):");
        line(out, "    key-v2 becomes  " + prefix + " + " + (hash.startsWith(prefix) ? hash.length() - prefix.length() : -1)
                + " hex characters (not shown)");
        line(out, "    that exact hash is in the audit trail: " + audit.contains(hash));
    }

    private static boolean connects(BaoLab lab, String user, String password) {
        try (Connection ignored = DriverManager.getConnection(lab.jdbcUrl(), user, password)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean canRead(BaoLab lab, String user, String password, String table) {
        try (Connection c = DriverManager.getConnection(lab.jdbcUrl(), user, password); Statement s = c.createStatement()) {
            s.executeQuery("SELECT * FROM " + table).close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void act3LeasedLogins(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "THREE. Database logins that are leased.");
        line(out, "  THE ROLE shop-api (what OpenBao runs to make each login):");
        for (String statement : CREATE.split("; ")) {
            int until = statement.indexOf(" VALID UNTIL");
            if (until > 0) {
                line(out, "    " + statement.substring(0, until));
                line(out, "      " + statement.substring(until + 1) + (statement.endsWith(";") ? "" : ";"));
            } else {
                line(out, "    " + statement + (statement.endsWith(";") ? "" : ";"));
            }
        }
        line(out, "    time to live 8 s, maximum 20 s");
        Bao shop = login(root, "shop-api");
        Bao reports = login(root, "reports");
        Bao.Reply a = shop.get("database/creds/shop-api");
        Bao.Reply b = reports.get("database/creds/reports");
        String userA = String.valueOf(a.data().get("username"));
        String userB = String.valueOf(b.data().get("username"));
        String passA = String.valueOf(a.data().get("password"));
        line(out, "  THE REQUESTS: GET database/creds/shop-api, .../reports");
        line(out, "  THE RESPONSES (names end in random letters, not shown):");
        line(out, "    shop-api  v-approle-shop-api-...: " + userA.startsWith("v-approle-shop-api-") + ", lease " + a.json().get("lease_duration") + " s");
        line(out, "    reports   v-approle-reports-...: " + userB.startsWith("v-approle-reports-") + ", a different login: " + !userA.equals(userB));
        line(out, "  WHAT THE LOGINS CAN DO IN THE REAL DATABASE:");
        line(out, "    both connect                 " + connects(lab, userA, passA) + ", " + connects(lab, userB, String.valueOf(b.data().get("password"))));
        line(out, "    shop-api reads orders        " + canRead(lab, userA, passA, "orders"));
        line(out, "    shop-api reads card_numbers  " + canRead(lab, userA, passA, "card_numbers"));
        String lease = String.valueOf(a.json().get("lease_id"));
        Bao.Reply renewed = root.put("sys/leases/renew", Map.of("lease_id", lease, "increment", 600));
        line(out, "  THE SHOP RENEWS AND ASKS FOR 600 SECONDS:");
        line(out, "    status   " + outcome(renewed.status()));
        line(out, "    granted  min(600, 20 - its age) s, so at most 20: "
                + (((Number) renewed.json().get("lease_duration")).intValue() <= 20));
        Thread.sleep(9_500);
        line(out, "  AFTER 9.5 SECONDS (past the 8 s time to live):");
        line(out, "    reports, never renewed, connects  " + connects(lab, userB, String.valueOf(b.data().get("password"))));
        line(out, "    shop-api, renewed, connects       " + connects(lab, userA, passA));
        line(out, "  THE OPERATOR REVOKES THE SHOP'S LEASE:");
        line(out, "    status   " + outcome(root.put("sys/leases/revoke", Map.of("lease_id", lease)).status())
                + ", the login connects: " + connects(lab, userA, passA));
    }

    private static void act4RotationAndBlastRadius(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "FOUR. Rotation, and the blast radius of a leak.");
        String before = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        line(out, "  BEFORE THE ROTATION (the static login payments):");
        line(out, "    a thief copies its password, which connects: " + connects(lab, "payments", before));
        root.post("database/rotate-role/payments", Map.of());
        String after = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        line(out, "  AFTER THE ROTATION (POST database/rotate-role/payments):");
        line(out, "    the new password differs   " + !before.equals(after));
        line(out, "    the new password connects  " + connects(lab, "payments", after));
        line(out, "    the thief's copy connects  " + connects(lab, "payments", before));
        Bao shop = login(root, "shop-api");
        List<Bao.Reply> leased = List.of(shop.get("database/creds/shop-api"), shop.get("database/creds/shop-api"), shop.get("database/creds/shop-api"));
        boolean allWork = leased.stream().allMatch(r -> connects(lab, String.valueOf(r.data().get("username")), String.valueOf(r.data().get("password"))));
        line(out, "  BEFORE THE REVOCATION (3 leased logins of shop-api):");
        line(out, "    all connect, and a thief copies one: " + allWork);
        line(out, "  AFTER REVOKING EVERY LEASE UNDER database/creds/shop-api:");
        line(out, "    status                     " + outcome(root.put("sys/leases/revoke-prefix/database/creds/shop-api", Map.of()).status()));
        long stillWork = leased.stream().filter(r -> connects(lab, String.valueOf(r.data().get("username")), String.valueOf(r.data().get("password")))).count();
        line(out, "    logins that still connect  " + stillWork + " of " + leased.size());
        line(out, "    reports still gets one     " + outcome(login(root, "reports").get("database/creds/reports").status()));
    }

    private static void act5Bill(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "FIVE. The bill.");
        Bao shop = login(root, "shop-api");
        Bao.Reply login = shop.get("database/creds/shop-api");
        String user = String.valueOf(login.data().get("username"));
        String password = String.valueOf(login.data().get("password"));
        String key = lab.unsealKey();
        line(out, "  THE OPERATOR SEALS OpenBao, AS IN AN INCIDENT:");
        line(out, "    status                  " + outcome(root.put("sys/seal", Map.of()).status()));
        line(out, "  WHILE IT IS SEALED:");
        line(out, "    the payment key         " + outcome(shop.get("secret/data/shop-api/payment").status()));
        line(out, "    a new login             " + outcome(shop.get("database/creds/shop-api").status()) + "  <- NO NEW SERVER STARTS");
        line(out, "    a login it already has  connects: " + connects(lab, user, password));
        line(out, "  AFTER UNSEALING WITH THE KEY:");
        line(out, "    status                  " + outcome(root.withToken(null).put("sys/unseal", Map.of("key", key)).status()));
        line(out, "    the payment key         " + outcome(shop.get("secret/data/shop-api/payment").status()));
        line(out, "  WHAT IT COSTS:");
        line(out, "    OpenBao holds every secret: guard, audit and back it up");
        line(out, "    this dev server keeps all in memory; a real one stores it");
        line(out, "    the first credential, role id and secret id, remains");
        line(out, "    applications must take a new login without a restart");
    }
}
