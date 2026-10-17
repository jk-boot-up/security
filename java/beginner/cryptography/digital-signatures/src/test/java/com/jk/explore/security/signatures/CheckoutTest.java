package com.jk.explore.security.signatures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private final Publisher shop = new Publisher(SignaturesDemo.shopPrivate());

    @Test
    void aGenuineListIsAccepted() {
        assertEquals(Verdict.OK, new Checkout(SignaturesDemo.shopPublic(), false).check(shop.publish(SignaturesDemo.list(2, 100, 1200, 25)), 50));
    }

    @Test
    void aChangedListWithTheOldSignatureIsRefused() {
        Signed signed = shop.publish(SignaturesDemo.list(2, 100, 1200, 25));
        Signed changed = new Signed(SignaturesDemo.list(2, 100, 12, 25), signed.signature());
        assertEquals(Verdict.BAD_SIGNATURE, new Checkout(SignaturesDemo.shopPublic(), false).check(changed, 50));
    }

    @Test
    void aListSignedByAnotherKeyIsRefused() {
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(SignaturesDemo.list(2, 100, 12, 25));
        assertEquals(Verdict.BAD_SIGNATURE, new Checkout(SignaturesDemo.shopPublic(), false).check(forged, 50));
    }

    @Test
    void theSameForgeryIsAcceptedByACheckoutThatTrustsTheWrongKey() {
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(SignaturesDemo.list(9, 100, 1, 1));
        assertEquals(Verdict.OK, new Checkout(mallory.getPublic(), false).check(forged, 50));
    }

    @Test
    void anOldListIsAcceptedWithoutAFreshnessCheckAndRefusedWithOne() {
        Signed old = shop.publish(SignaturesDemo.list(1, 100, 1000, 20));
        Signed current = shop.publish(SignaturesDemo.list(2, 100, 1200, 25));
        Checkout naive = new Checkout(SignaturesDemo.shopPublic(), false);
        naive.check(current, 50);
        assertEquals(Verdict.OK, naive.check(old, 50));
        Checkout careful = new Checkout(SignaturesDemo.shopPublic(), true);
        careful.check(current, 50);
        assertEquals(Verdict.OLD_VERSION, careful.check(old, 50));
    }

    @Test
    void anExpiredListIsRefusedWhenFreshnessIsChecked() {
        Signed shortLived = shop.publish(SignaturesDemo.list(1, 40, 1000, 20));
        assertEquals(Verdict.EXPIRED, new Checkout(SignaturesDemo.shopPublic(), true).check(shortLived, 50));
        assertEquals(Verdict.OK, new Checkout(SignaturesDemo.shopPublic(), true).check(shortLived, 40));
    }

    @Test
    void aListSignedForAnotherPurposeIsRefused() {
        byte[] bytes = SignaturesDemo.list(2, 100, 1200, 25).encode();
        byte[] refundSignature = Signing.sign(SignaturesDemo.shopPrivate(), "shop.refund.v1", bytes);
        assertEquals(Verdict.BAD_SIGNATURE, new Checkout(SignaturesDemo.shopPublic(), false)
                .check(new Signed(SignaturesDemo.list(2, 100, 1200, 25), refundSignature), 50));
    }
}
