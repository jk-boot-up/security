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
            root.put("sys/policies/acl/" + app, Map.of("policy", "path \"secret/data/" + app + "/*\" { capabilities = [\"read\"] }\npath \"database/creds/" + app + "\" { capabilities = [\"read\"] }"));
            root.post("auth/approle/role/" + app, Map.of("token_policies", app, "token_ttl", "120s", "secret_id_num_uses", 1, "secret_id_ttl", "60s"));
            root.post("database/roles/" + app, Map.of("db_name", "shopdb", "creation_statements", CREATE, "renew_statements", RENEW, "default_ttl", "8s", "max_ttl", "20s"));
        }
        root.post("secret/data/shop-api/payment", Map.of("data", Map.of("api-key", "key-v1")));
        root.post("secret/data/reports/warehouse", Map.of("data", Map.of("api-key", "warehouse-key")));
        root.post("database/static-roles/payments", Map.of("db_name", "shopdb", "username", "payments", "rotation_period", "24h"));
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
        return status == 200 || status == 204 ? "allowed" : "refused (" + status + ")";
    }

    private static void act1WhoMayAsk(StringBuilder out, Bao root) throws Exception {
        line(out, "ONE. A secrets server, and who may ask.");
        line(out, "  the store's payment key lives in OpenBao, not in the code, the environment or the image; two applications each have a role, a policy and a secret id that works once");
        String secretId = newSecretId(root, "shop-api");
        Bao.Reply first = loginReply(root, "shop-api", secretId);
        line(out, "  shop-api logs in with its role id and its secret id: " + outcome(first.status()) + ", a token with the policies " + first.auth().get("policies") + " for " + first.auth().get("lease_duration") + " seconds");
        line(out, "  the same secret id again, as a thief who saw it would use it: " + outcome(loginReply(root, "shop-api", secretId).status()));
        line(out, "  a secret id nobody issued: " + outcome(loginReply(root, "shop-api", "00000000-0000-0000-0000-000000000000").status()));
        Bao shop = root.withToken(String.valueOf(first.auth().get("client_token")));
        Bao reports = login(root, "reports");
        line(out, "  shop-api reads its payment key: " + outcome(shop.get("secret/data/shop-api/payment").status()) + ", the reports key: " + outcome(shop.get("secret/data/reports/warehouse").status()));
        line(out, "  reports reads its own key: " + outcome(reports.get("secret/data/reports/warehouse").status()) + ", the payment key: " + outcome(reports.get("secret/data/shop-api/payment").status()));
        line(out, "  a request with no token: " + outcome(root.withToken(null).get("secret/data/shop-api/payment").status()));
        line(out, "  a policy names exactly what each application may read, and everything else is refused");
    }

    private static void act2StaticVersions(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "TWO. Secrets that stay still: versions, rotation and the audit trail.");
        Bao shop = login(root, "shop-api");
        line(out, "  the store reads the payment key: " + shop.get("secret/data/shop-api/payment").data().get("data"));
        root.post("secret/data/shop-api/payment", Map.of("data", Map.of("api-key", "key-v2")));
        line(out, "  the provider issues v2 and it is written as version 2; during the changeover both are readable by number: v1 " + shop.get("secret/data/shop-api/payment?version=1").data().get("data")
                + ", v2 " + shop.get("secret/data/shop-api/payment?version=2").data().get("data"));
        line(out, "  the store, asking for the current version, now gets " + shop.get("secret/data/shop-api/payment").data().get("data"));
        line(out, "  the application cannot destroy versions: " + outcome(shop.put("secret/destroy/shop-api/payment", Map.of("versions", List.of(1))).status()));
        line(out, "  after every server has switched, the operator destroys v1: " + outcome(root.put("secret/destroy/shop-api/payment", Map.of("versions", List.of(1))).status())
                + ", and reading v1 now: " + outcome(shop.get("secret/data/shop-api/payment?version=1").status()) + ", v2: " + outcome(shop.get("secret/data/shop-api/payment?version=2").status()));
        String audit = lab.auditLog();
        line(out, "  the audit trail recorded the requests: " + audit.contains("secret/data/shop-api/payment") + ", the application that asked: " + audit.contains("shop-api")
                + ", the key itself: " + (audit.contains("key-v1") || audit.contains("key-v2")) + "  (values are written as HMACs: " + audit.contains("hmac-sha256:") + ")");
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
        Bao shop = login(root, "shop-api");
        Bao reports = login(root, "reports");
        Bao.Reply a = shop.get("database/creds/shop-api");
        Bao.Reply b = reports.get("database/creds/reports");
        String userA = String.valueOf(a.data().get("username"));
        String userB = String.valueOf(b.data().get("username"));
        line(out, "  shop-api asks and gets a database login of its own, named v-approle-shop-api-<random>: " + userA.startsWith("v-approle-shop-api-") + ", for " + a.json().get("lease_duration") + " seconds");
        line(out, "  reports gets a different one: " + userB.startsWith("v-approle-reports-") + "; no two applications share one: " + !userA.equals(userB));
        line(out, "  both connect to the real database: " + connects(lab, userA, String.valueOf(a.data().get("password"))) + ", " + connects(lab, userB, String.valueOf(b.data().get("password"))));
        line(out, "  shop-api's login reads orders: " + canRead(lab, userA, String.valueOf(a.data().get("password")), "orders") + ", and the card numbers: " + canRead(lab, userA, String.valueOf(a.data().get("password")), "card_numbers"));
        line(out, "  shop-api asks for reports' login: " + outcome(shop.get("database/creds/reports").status()));
        String lease = String.valueOf(a.json().get("lease_id"));
        Bao.Reply renewed = root.put("sys/leases/renew", Map.of("lease_id", lease, "increment", 600));
        line(out, "  the shop renews its lease and asks for 600 seconds: " + outcome(renewed.status()) + ", it is capped at the maximum of 20 seconds: " + (((Number) renewed.json().get("lease_duration")).intValue() <= 20));
        Thread.sleep(9_500);
        line(out, "  reports never renewed; 9 seconds on its lease has ended and the login is removed: can it connect? " + connects(lab, userB, String.valueOf(b.data().get("password"))));
        line(out, "  the shop's renewed login still connects: " + connects(lab, userA, String.valueOf(a.data().get("password"))));
        line(out, "  the operator revokes the shop's lease: " + outcome(root.put("sys/leases/revoke", Map.of("lease_id", lease)).status()) + ", and the login connects: " + connects(lab, userA, String.valueOf(a.data().get("password"))));
    }

    private static void act4RotationAndBlastRadius(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "FOUR. Rotation, and the blast radius of a leak.");
        String before = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        line(out, "  the payments service has a static login that OpenBao manages; a thief copies its password, which connects: " + connects(lab, "payments", before));
        root.post("database/rotate-role/payments", Map.of());
        String after = String.valueOf(root.get("database/static-creds/payments").data().get("password"));
        line(out, "  OpenBao rotates it: the new password differs: " + !before.equals(after) + ", the new one connects: " + connects(lab, "payments", after) + ", the thief's copy: " + connects(lab, "payments", before));
        Bao shop = login(root, "shop-api");
        List<Bao.Reply> leased = List.of(shop.get("database/creds/shop-api"), shop.get("database/creds/shop-api"), shop.get("database/creds/shop-api"));
        boolean allWork = leased.stream().allMatch(r -> connects(lab, String.valueOf(r.data().get("username")), String.valueOf(r.data().get("password"))));
        line(out, "  three logins leased to shop-api's servers all connect: " + allWork + "; a thief copies one");
        line(out, "  the leak is noticed: the operator revokes every lease under database/creds/shop-api at once: " + outcome(root.put("sys/leases/revoke-prefix/database/creds/shop-api", Map.of()).status()));
        long stillWork = leased.stream().filter(r -> connects(lab, String.valueOf(r.data().get("username")), String.valueOf(r.data().get("password")))).count();
        line(out, "  logins that still connect: " + stillWork + "  (a leased login ends with its lease; a static password lasts until someone changes it)");
        line(out, "  the other applications are untouched: reports still gets a login: " + outcome(login(root, "reports").get("database/creds/reports").status()));
    }

    private static void act5Bill(StringBuilder out, BaoLab lab, Bao root) throws Exception {
        line(out, "FIVE. The bill.");
        Bao shop = login(root, "shop-api");
        Bao.Reply login = shop.get("database/creds/shop-api");
        String user = String.valueOf(login.data().get("username"));
        String password = String.valueOf(login.data().get("password"));
        String key = lab.unsealKey();
        line(out, "  the operator seals OpenBao, as in an incident: " + outcome(root.put("sys/seal", Map.of()).status()));
        line(out, "  the application asks for its payment key: " + outcome(shop.get("secret/data/shop-api/payment").status()) + ", for a new login: " + outcome(shop.get("database/creds/shop-api").status())
                + "  <- THE STORE CANNOT START A SERVER WHILE THE SECRETS SERVICE IS DOWN");
        line(out, "  a login it already holds keeps working until its lease would have ended: " + connects(lab, user, password));
        line(out, "  the operator unseals with the key: " + outcome(root.withToken(null).put("sys/unseal", Map.of("key", key)).status()) + ", and the application reads again: " + outcome(shop.get("secret/data/shop-api/payment").status()));
        line(out, "  the secrets service holds every secret, so it needs its own access control, audit and backups; this dev server keeps everything in memory and a real one does not");
        line(out, "  the application still needs a first credential, its role id and a secret id, to prove who it is; platform identity, in the advanced track, removes it");
        line(out, "  leases and rotation need applications that can reconnect with a new login without a restart");
    }
}
