package com.jk.explore.security.privacyeng;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** PostgreSQL in a Docker container, started for the demo and removed at the end. */
public final class Pg implements AutoCloseable {

    static final String IMAGE = "postgres:18.6-alpine";
    private static final String PASSWORD = "demo-only";

    private final String name = "privacy-" + UUID.randomUUID().toString().substring(0, 6);
    private final Connection connection;

    public Pg() throws IOException, InterruptedException, SQLException {
        Sh.must(List.of("docker", "run", "-d", "--rm", "--name", name, "-e", "POSTGRES_PASSWORD=" + PASSWORD, "-p", "127.0.0.1::5432", IMAGE));
        String hostPort = Sh.must(List.of("docker", "port", name, "5432/tcp")).lines().findFirst().orElseThrow().strip();
        String port = hostPort.substring(hostPort.lastIndexOf(':') + 1);
        SQLException last = null;
        Connection c = null;
        for (int i = 0; i < 60 && c == null; i++) {
            try {
                c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "postgres", PASSWORD);
            } catch (SQLException e) {
                last = e;
                Thread.sleep(1000);
            }
        }
        if (c == null) {
            close();
            throw last;
        }
        this.connection = c;
    }

    public Connection connection() {
        return connection;
    }

    public void execute(String sql) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute(sql);
        }
    }

    public long count(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<String> strings(String sql, Object... params) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    public int update(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            // closing anyway
        }
        try {
            Sh.run("docker", "rm", "-f", name);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
