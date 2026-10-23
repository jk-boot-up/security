package bank;

/** Query text. */
final class Queries {

    private Queries() {
    }

    static String byOwner(String owner) {
        return "SELECT * FROM accounts WHERE owner = '" + owner + "'";
    }
}
