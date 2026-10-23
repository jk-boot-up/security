package com.jk.explore.security.privacyeng;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** A made-up bank database: 2,000 customers with accounts, transactions and support notes, always the same because the generator is seeded. */
public final class Seed {

    public static final int CUSTOMERS = 2_000;
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    static final List<String> FIRST = List.of("Anna", "Ben", "Clara", "David", "Elif", "Felix", "Greta", "Hugo", "Ines", "Jonas", "Katja", "Leon", "Mira", "Nils", "Olga", "Paul", "Rosa", "Sven", "Tara", "Uwe");
    static final List<String> LAST = List.of("Becker", "Schmidt", "Wagner", "Fischer", "Keller", "Braun", "Hoffmann", "Vogel", "Lang", "Richter", "Krause", "Neumann", "Roth", "Winter", "Sommer");
    static final List<String> MERCHANTS = List.of("Corner Bakery", "City Transit", "Green Grocer", "Book Barn", "Power Utility", "Pharmacy Plus", "Cinema Nova", "Tech Depot");

    private Seed() {
    }

    public static String nameOf(int id) {
        return FIRST.get(id % FIRST.size()) + " " + LAST.get((id / FIRST.size()) % LAST.size()) + " " + id;
    }

    public static String emailOf(int id) {
        return "customer" + id + "@mail.example";
    }

    public static void create(Pg pg) throws SQLException {
        pg.execute("""
                CREATE TABLE customers (
                  id INT PRIMARY KEY, name TEXT, email TEXT, phone TEXT, postcode TEXT, birth_date DATE, gender TEXT, national_id TEXT,
                  marketing_consent BOOLEAN, consent_date DATE, legal_hold BOOLEAN DEFAULT FALSE)""");
        pg.execute("CREATE TABLE accounts (id INT PRIMARY KEY, customer_id INT REFERENCES customers(id), iban TEXT)");
        pg.execute("CREATE TABLE transactions (id INT PRIMARY KEY, account_id INT REFERENCES accounts(id), amount NUMERIC(10,2), booked_on DATE, counterparty_name TEXT)");
        pg.execute("CREATE TABLE support_notes (id INT PRIMARY KEY, customer_id INT REFERENCES customers(id), created_on DATE, note TEXT)");
        pg.execute("CREATE TABLE analytics_tx (pseudonym TEXT, amount_band TEXT, postcode TEXT, birth_date DATE, gender TEXT, booked_year INT)");
        Connection c = pg.connection();
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(20261023L);
        try (PreparedStatement cust = c.prepareStatement("INSERT INTO customers VALUES (?,?,?,?,?,?,?,?,?,?,?)");
             PreparedStatement acc = c.prepareStatement("INSERT INTO accounts VALUES (?,?,?)");
             PreparedStatement tx = c.prepareStatement("INSERT INTO transactions VALUES (?,?,?,?,?)");
             PreparedStatement note = c.prepareStatement("INSERT INTO support_notes VALUES (?,?,?,?)")) {
            int accountId = 0;
            int txId = 0;
            int noteId = 0;
            LocalDate birthStart = LocalDate.of(1950, 1, 1);
            for (int id = 1; id <= CUSTOMERS; id++) {
                String postcode = (100 + rng.nextInt(10)) + List.of("15", "45", "78").get(rng.nextInt(3));
                boolean consent = rng.nextInt(100) < 40;
                cust.setInt(1, id);
                cust.setString(2, nameOf(id));
                cust.setString(3, emailOf(id));
                cust.setString(4, "+49 30 " + (1_000_000 + rng.nextInt(9_000_000)));
                cust.setString(5, postcode);
                cust.setDate(6, Date.valueOf(birthStart.plusDays(rng.nextInt(20_454))));
                cust.setString(7, rng.nextBoolean() ? "F" : "M");
                cust.setString(8, String.format("%011d", 10_000_000_000L + rng.nextLong(80_000_000_000L)));
                cust.setBoolean(9, consent);
                cust.setDate(10, consent ? Date.valueOf(TODAY.minusDays(rng.nextInt(1_800))) : null);
                cust.setBoolean(11, id % 700 == 0);
                cust.addBatch();
            }
            cust.executeBatch();
            for (int id = 1; id <= CUSTOMERS; id++) {
                for (int a = 0; a < (id % 2 == 0 ? 2 : 1); a++) {
                    accountId++;
                    acc.setInt(1, accountId);
                    acc.setInt(2, id);
                    acc.setString(3, "DE" + String.format("%020d", accountId * 7919L));
                    acc.addBatch();
                }
            }
            acc.executeBatch();
            for (int a = 1; a <= accountId; a++) {
                int n = 3 + rng.nextInt(10);
                for (int t = 0; t < n; t++) {
                    txId++;
                    tx.setInt(1, txId);
                    tx.setInt(2, a);
                    tx.setBigDecimal(3, java.math.BigDecimal.valueOf(500 + rng.nextInt(500_000), 2));
                    tx.setDate(4, Date.valueOf(LocalDate.of(2015, 1, 1).plusDays(rng.nextInt(4_100))));
                    tx.setString(5, rng.nextInt(100) < 85 ? MERCHANTS.get(rng.nextInt(MERCHANTS.size())) : nameOf(1 + rng.nextInt(CUSTOMERS)));
                    tx.addBatch();
                }
            }
            tx.executeBatch();
            for (int id = 1; id <= CUSTOMERS; id++) {
                if (rng.nextInt(100) < 20) {
                    for (int k = 0; k < 1 + rng.nextInt(3); k++) {
                        noteId++;
                        note.setInt(1, noteId);
                        note.setInt(2, id);
                        note.setDate(3, Date.valueOf(LocalDate.of(2024, 1, 1).plusDays(rng.nextInt(1_000))));
                        note.setString(4, "Called about the card. Call back on the number in the file.");
                        note.addBatch();
                    }
                }
            }
            note.executeBatch();
            // customer 17 appears in other people's records: joint account notes and payments between people
            for (int other : new int[] {18, 19}) {
                noteId++;
                note.setInt(1, noteId);
                note.setInt(2, other);
                note.setDate(3, Date.valueOf(TODAY.minusDays(40)));
                note.setString(4, "Joint account request together with " + nameOf(17) + ", contact " + emailOf(17) + ".");
                note.executeUpdate();
            }
            for (int other : new int[] {20, 21, 22}) {
                txId++;
                tx.setInt(1, txId);
                tx.setInt(2, other * 2);
                tx.setBigDecimal(3, java.math.BigDecimal.valueOf(12_000, 2));
                tx.setDate(4, Date.valueOf(TODAY.minusDays(60)));
                tx.setString(5, nameOf(17));
                tx.executeUpdate();
            }
        }
    }
}
