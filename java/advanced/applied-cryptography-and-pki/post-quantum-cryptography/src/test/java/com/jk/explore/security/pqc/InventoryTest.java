package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void aFileIsFiledUnderTheFirstUseInTheMigrationOrder() {
        List<Inventory.Entry> e = Inventory.scan(List.of(
                new Inventory.Source("a", "KeyAgreement.getInstance(\"ECDH\"); Cipher.getInstance(\"AES/GCM/NoPadding\");"),
                new Inventory.Source("b", "Signature.getInstance(\"SHA256withRSA\"); Mac.getInstance(\"HmacSHA256\");"),
                new Inventory.Source("c", "Mac.getInstance(\"HmacSHA256\");")));
        assertEquals(Inventory.Use.KEY_EXCHANGE, e.get(0).first());
        assertEquals(Inventory.Use.SIGNATURE, e.get(1).first());
        assertEquals(Inventory.Use.SYMMETRIC, e.get(2).first());
        assertEquals(List.of("ECDH", "AES"), e.get(0).found().stream().map(Inventory.Algorithm::name).toList());
    }

    @Test
    void rsaIsReadAsKeyTransportOrAsASignatureByHowItIsNamed() {
        List<Inventory.Entry> e = Inventory.scan(List.of(
                new Inventory.Source("wrap", "Cipher.getInstance(\"RSA/ECB/OAEPWithSHA-256AndMGF1Padding\");"),
                new Inventory.Source("sign", "Signature.getInstance(\"SHA256withRSA\");")));
        assertEquals(Inventory.Use.KEY_EXCHANGE, e.get(0).first());
        assertEquals(Inventory.Use.SIGNATURE, e.get(1).first());
    }

    @Test
    void theBankSampleHasThreeFilesOfEachPublicKeyUseAndOneSymmetricOnly() {
        List<Inventory.Entry> e = Inventory.scan(Inventory.BANK_SOURCES);
        assertEquals(3, Inventory.count(e, Inventory.Use.KEY_EXCHANGE));
        assertEquals(3, Inventory.count(e, Inventory.Use.SIGNATURE));
        assertEquals(1, Inventory.count(e, Inventory.Use.SYMMETRIC));
    }
}
