package bank;

/** SessionTokens: part of the bank's code base, kept here as text for the inventory. It is not compiled. */
class SessionTokens {

    void use() throws Exception {
        var x0 = Mac.getInstance("HmacSHA256");
        var x1 = SecureRandom.getInstance("SHA1PRNG");
    }
}
