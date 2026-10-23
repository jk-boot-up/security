package bank;

/** StatementSigner: part of the bank's code base, kept here as text for the inventory. It is not compiled. */
class StatementSigner {

    void use() throws Exception {
        var x0 = Signature.getInstance("SHA256withRSA");
        var x1 = MessageDigest.getInstance("SHA-1");
    }
}
