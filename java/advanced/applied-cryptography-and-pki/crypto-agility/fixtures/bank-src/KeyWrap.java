package bank;

/** KeyWrap: part of the bank's code base, kept here as text for the inventory. It is not compiled. */
class KeyWrap {

    void use() throws Exception {
        var x0 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        var x1 = Cipher.getInstance("AES/GCM/NoPadding");
    }
}
