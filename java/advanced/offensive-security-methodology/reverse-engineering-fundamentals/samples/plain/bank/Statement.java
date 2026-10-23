package bank;

/** Formats the lines of a statement. */
public final class Statement {

    private Statement() {
    }

    public static String line(String payee, long cents) {
        return String.format("%-20s %10d.%02d", payee, cents / 100, cents % 100);
    }
}
