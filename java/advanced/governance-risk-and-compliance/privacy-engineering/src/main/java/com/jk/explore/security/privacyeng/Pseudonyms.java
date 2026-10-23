package com.jk.explore.security.privacyeng;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Keyed pseudonyms for analytics: the same customer always gets the same one under one key, and nobody can compute it without the key. */
public final class Pseudonyms {

    private final byte[] key;

    public Pseudonyms(String secret) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String of(int customerId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("customer:" + customerId).getBytes(StandardCharsets.UTF_8)), 0, 8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Fills the analytics table: one row for each transaction, with a pseudonym, an amount band, and the values that make a person findable. */
    public int publish(Pg pg) throws SQLException {
        int rows = 0;
        try (var read = pg.connection().createStatement();
             var rs = read.executeQuery("SELECT a.customer_id, t.amount, t.booked_on, c.postcode, c.birth_date, c.gender FROM transactions t JOIN accounts a ON a.id = t.account_id JOIN customers c ON c.id = a.customer_id");
             var ins = pg.connection().prepareStatement("INSERT INTO analytics_tx VALUES (?,?,?,?,?,?)")) {
            while (rs.next()) {
                ins.setString(1, of(rs.getInt(1)));
                ins.setString(2, rs.getBigDecimal(2).intValue() < 100 ? "under 100" : rs.getBigDecimal(2).intValue() < 1000 ? "100 to 999" : "1000 and over");
                ins.setString(3, rs.getString(4));
                ins.setDate(4, rs.getDate(5));
                ins.setString(5, rs.getString(6));
                ins.setInt(6, rs.getDate(3).toLocalDate().getYear());
                ins.addBatch();
                rows++;
            }
            ins.executeBatch();
        }
        return rows;
    }
}
