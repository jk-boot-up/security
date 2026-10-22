package com.jk.explore.security.racepg;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** A real PostgreSQL 18 in a container on a free loopback port, removed on close, and the small things every act needs from it. */
public final class Db implements AutoCloseable {

    static final String IMAGE = "postgres:18.6-alpine";
    static final String PASSWORD = "lab-only";

    private final String container = "race-db-" + UUID.randomUUID().toString().substring(0, 8);
    private final int port;

    public Db() throws IOException, InterruptedException, SQLException {
        run("docker", "run", "-d", "--rm", "--name", container, "-p", "127.0.0.1::5432", "-e", "POSTGRES_PASSWORD=" + PASSWORD, IMAGE,
                "-c", "deadlock_timeout=200ms", "-c", "max_connections=60", "-c", "fsync=off");
        String mapping = run("docker", "port", container, "5432").lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        this.port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).strip());
        for (int i = 0; i < 200; i++) {
            try (Connection c = connect("ready")) {
                c.createStatement().execute("select 1");
                return;
            } catch (SQLException e) {
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("postgres did not start: " + run("docker", "logs", container));
    }

    String url() {
        return "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
    }

    /** A connection with transactions on (no autocommit), named so that pg_stat_activity can say who waits. */
    Connection connect(String name) throws SQLException {
        Properties p = new Properties();
        p.setProperty("user", "postgres");
        p.setProperty("password", PASSWORD);
        p.setProperty("ApplicationName", name);
        Connection c = DriverManager.getConnection(url(), p);
        c.setAutoCommit(false);
        return c;
    }

    /** Runs statements on a connection of its own and commits. */
    void exec(String... statements) throws SQLException {
        try (Connection c = connect("setup"); Statement s = c.createStatement()) {
            for (String sql : statements) {
                s.execute(sql);
            }
            c.commit();
        }
    }

    /** The tables of the lesson, empty and ready. */
    void reset() throws SQLException {
        exec("drop table if exists accounts, coupons, payments, household cascade",
                "create table accounts (id text primary key, balance int not null check (balance >= 0), paid_out int not null default 0)",
                "insert into accounts values ('ANA-1', 100, 0), ('BEN-2', 100, 0)",
                "create table coupons (code text primary key, used boolean not null default false, discount_given int not null default 0)",
                "insert into coupons values ('SPRING20', false, 0)",
                "create table payments (key text primary key, amount int not null)",
                "create table household (id text primary key, balance int not null)",
                "insert into household values ('ANA-1', 100), ('ANA-2', 100)");
    }

    static int one(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps.executeUpdate();
        }
    }

    /** How many sessions with this application name are waiting for a lock right now. */
    int waiting(String applicationName) throws SQLException {
        try (Connection c = connect("observer")) {
            return one(c, "select count(*) from pg_stat_activity where application_name = ? and wait_event_type = 'Lock'", applicationName);
        }
    }

    /** Waits until the named session is waiting for a lock, or gives up after a few seconds. */
    boolean waitUntilWaiting(String applicationName) throws SQLException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (waiting(applicationName) > 0) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    static List<String> names(SQLException e) {
        return List.of(e.getSQLState());
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(120, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    @Override
    public void close() {
        try {
            run("docker", "rm", "-f", container);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // already gone
        }
    }
}
