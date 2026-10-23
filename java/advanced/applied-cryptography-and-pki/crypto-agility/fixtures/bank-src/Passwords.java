package bank;

/** Passwords: part of the bank's code base, kept here as text for the inventory. It is not compiled. */
class Passwords {

    void use() throws Exception {
        var x0 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var x1 = MessageDigest.getInstance("SHA-256");
    }
}
