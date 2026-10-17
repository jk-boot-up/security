package com.jk.explore.security.signatures;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class SignaturesDemo {

    /** The shop's key: the test key of RFC 8032 (test 1), so signatures are the same on every run. It is public, and must never protect anything real. */
    static final byte[] SHOP_SEED = Hex.parse("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
    static final byte[] SHOP_PUBLIC = Hex.parse("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");

    static PrivateKey shopPrivate() {
        return Ed25519Keys.privateFromSeed(SHOP_SEED);
    }

    static PublicKey shopPublic() {
        return Ed25519Keys.publicFromRaw(SHOP_PUBLIC);
    }

    static PriceList list(int version, int validUntilDay, int laptop, int mouse) {
        Map<String, Integer> prices = new LinkedHashMap<>();
        prices.put("mouse", mouse);
        prices.put("laptop", laptop);
        return new PriceList(version, validUntilDay, prices);
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1SharedKey(out);
        act2Sign(out);
        act3Purpose(out);
        act4Freshness(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String shortHex(byte[] bytes) {
        return Hex.of(bytes).substring(0, 16);
    }

    private static void act1SharedKey(StringBuilder out) {
        line(out, "ONE. A tag made with a shared key.");
        byte[] key = Hex.bytes("demo-shared-key-not-for-real-use");
        HmacPublisher shop = new HmacPublisher(key);
        HmacPublisher warehouse = new HmacPublisher(key);
        PriceList real = list(2, 100, 1200, 25);
        PriceList forged = list(3, 100, 12, 25);
        line(out, "  the shop tags its price list with HMAC: " + shortHex(shop.tag(real)));
        line(out, "  the warehouse must be able to check the tag, so it holds the same key");
        line(out, "  the warehouse tags a list of its own, with the laptop at 12 dollars: "
                + (shop.accepts(forged, warehouse.tag(forged)) ? "accepted" : "refused") + "  <- ANYONE WHO CAN CHECK CAN FORGE");
    }

    private static void act2Sign(StringBuilder out) {
        line(out, "TWO. A signature: made with the private key, checked with the public key.");
        Publisher shop = new Publisher(shopPrivate());
        PriceList real = list(2, 100, 1200, 25);
        Signed signed = shop.publish(real);
        Checkout warehouse = new Checkout(shopPublic(), false);
        line(out, "  the shop signs its price list: " + signed.signature().length + " bytes, beginning " + shortHex(signed.signature()));
        line(out, "  signing it again gives the same bytes: "
                + Arrays.equals(signed.signature(), shop.publish(real).signature()) + "  <- NO RANDOM NUMBER IS NEEDED");
        line(out, "  the warehouse holds only the public key and checks it: " + warehouse.check(signed, 50));
        Signed changed = new Signed(list(2, 100, 12, 25), signed.signature());
        line(out, "  the laptop is changed to 12 dollars, the signature kept: " + warehouse.check(changed, 50));
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(list(2, 100, 12, 25));
        line(out, "  Mallory signs her own list with her own key: " + warehouse.check(forged, 50));
        line(out, "  the warehouse can check but cannot make one: no key it holds can sign for the shop");
    }

    private static void act3Purpose(StringBuilder out) {
        line(out, "THREE. What exactly is signed.");
        PriceList list = list(2, 100, 1200, 25);
        PriceList sameButBuiltDifferently = new PriceList(2, 100, new java.util.TreeMap<>(Map.of("laptop", 1200, "mouse", 25)));
        line(out, "  two lists with the same products, built in a different order, give the same bytes: "
                + Arrays.equals(list.encode(), sameButBuiltDifferently.encode()));
        byte[] signature = Signing.sign(shopPrivate(), "shop.refund.v1", list.encode());
        line(out, "  the shop signs the same bytes for another purpose, a refund order, with the label shop.refund.v1");
        line(out, "  that signature, checked as a refund order: " + Signing.verify(shopPublic(), "shop.refund.v1", list.encode(), signature));
        line(out, "  that signature, offered as a price list: "
                + (Signing.verify(shopPublic(), Publisher.CONTEXT, list.encode(), signature) ? "accepted" : "refused")
                + "  <- A SIGNATURE IS TIED TO ITS PURPOSE");
        line(out, "  the message and the label are separated by a zero byte, so one cannot run into the other");
    }

    private static void act4Freshness(StringBuilder out) {
        line(out, "FOUR. A good signature on an old message.");
        Publisher shop = new Publisher(shopPrivate());
        Signed old = shop.publish(list(1, 100, 1000, 20));
        Signed current = shop.publish(list(2, 100, 1200, 25));
        Checkout naive = new Checkout(shopPublic(), false);
        naive.check(current, 50);
        line(out, "  the shop published version 1 (a laptop at 1000), then version 2 (at 1200)");
        line(out, "  an attacker replays the old, genuinely signed, version 1 on day 50: " + naive.check(old, 50)
                + "  <- THE SIGNATURE IS PERFECT, THE LIST IS OLD");
        Checkout careful = new Checkout(shopPublic(), true);
        careful.check(current, 50);
        line(out, "  a checkout that remembers the newest version: " + careful.check(old, 50));
        Signed shortLived = shop.publish(list(1, 40, 1000, 20));
        line(out, "  a list that was only good until day 40, offered on day 50: " + new Checkout(shopPublic(), true).check(shortLived, 50));
        line(out, "  the version and the expiry are inside what is signed, so nobody can change them");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: whose key is it, and what if it is stolen?");
        PriceList forgedList = list(9, 100, 1, 1);
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(forgedList);
        Checkout fooled = new Checkout(mallory.getPublic(), false);
        line(out, "  Mallory gives the checkout her public key and says it is the shop's: "
                + fooled.check(forged, 50) + "  <- THE CHECK IS ONLY AS GOOD AS THE KEY");
        Checkout right = new Checkout(shopPublic(), false);
        line(out, "  with the shop's real key in the checkout: " + right.check(forged, 50));
        Signed stolen = new Publisher(shopPrivate()).publish(forgedList);
        line(out, "  if the shop's private key is stolen, the thief's list is accepted: " + right.check(stolen, 50)
                + "  <- UNTIL THE KEY IS REVOKED");
        line(out, "  a signature hides nothing: the price list is readable by anyone, signed or not");
        line(out, "  sizes: a signature is " + Signing.SIGNATURE_BYTES + " bytes, a raw public key " + SHOP_PUBLIC.length
                + ", and the same key encoded " + shopPublic().getEncoded().length);
    }
}
