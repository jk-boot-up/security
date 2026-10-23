package bank;

/** The licence check of a bank's desktop client: the key is compared on the customer's own computer. */
public final class Licence {

    private static final String VENDOR_KEY = "bankapp-demo-key-0001";
    private static final String SERVER = "https://licence.bank.example/check";

    private Licence() {
    }

    public static boolean check(String key) {
        if (key == null) {
            return false;
        }
        if (VENDOR_KEY.equals(key)) {
            return true;
        }
        return false;
    }

    public static String describe() {
        return "BankApp client, licence server " + SERVER;
    }
}
