package com.jk.explore.security.symmetric;

/**
 * Nonces that are different every time but the same on every run: 1, 2, 3 and so on. They keep the demo's output repeatable,
 * and are safe only because nothing here ever asks for more than a handful. They are not random and must not be used for real data.
 */
public class CountingNonces implements NonceSource {

    private int counter;

    @Override
    public byte[] next() {
        byte[] nonce = new byte[12];
        counter++;
        nonce[8] = (byte) (counter >>> 24);
        nonce[9] = (byte) (counter >>> 16);
        nonce[10] = (byte) (counter >>> 8);
        nonce[11] = (byte) counter;
        return nonce;
    }
}
