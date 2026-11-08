package com.jk.explore.security.signatures;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs the online store story in six numbered acts and prints exact, repeatable results. */
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
        act3Steps(out);
        act4Purpose(out);
        act5Freshness(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The list's bytes as one line of text: its lines joined by spaces. */
    private static String text(PriceList list) {
        return new String(list.encode(), java.nio.charset.StandardCharsets.UTF_8).strip().replace('\n', ' ');
    }

    private static String first16(byte[] bytes) {
        return Hex.grouped(bytes, 0, 16) + " ...";
    }

    private static void act1SharedKey(StringBuilder out) {
        line(out, "ONE. A tag made with a shared key.");
        byte[] key = Hex.bytes("demo-shared-key-not-for-real-use");
        HmacPublisher shop = new HmacPublisher(key);
        HmacPublisher warehouse = new HmacPublisher(key);
        PriceList real = list(2, 100, 1200, 25);
        PriceList forged = list(3, 100, 12, 25);
        line(out, "  BEFORE TAGGING (the inputs):");
        line(out, "    price list   " + text(real));
        line(out, "    shared key   " + new String(key, java.nio.charset.StandardCharsets.UTF_8));
        line(out, "  AFTER TAGGING (HMAC-SHA-256, 32 bytes):");
        line(out, "    tag          " + first16(shop.tag(real)));
        line(out, "  THE ATTACK (the warehouse holds the same key):");
        line(out, "    its list     " + text(forged));
        byte[] forgedTag = warehouse.tag(forged);
        line(out, "    its tag      " + first16(forgedTag));
        line(out, "    the shop's check: " + (shop.accepts(forged, forgedTag) ? "accepted" : "refused")
                + "  <- ANYONE WHO CAN CHECK CAN FORGE");
    }

    private static void act2Sign(StringBuilder out) {
        line(out, "TWO. A signature: the private key signs, the public key checks.");
        Publisher shop = new Publisher(shopPrivate());
        PriceList real = list(2, 100, 1200, 25);
        Signed signed = shop.publish(real);
        byte[] sig = signed.signature();
        line(out, "  BEFORE SIGNING (the inputs):");
        line(out, "    label        " + Publisher.CONTEXT);
        line(out, "    price list   " + text(real));
        line(out, "    private key  " + Hex.grouped(SHOP_SEED, 0, 8) + " ...  (only the shop)");
        line(out, "  AFTER SIGNING (the signature, " + sig.length + " bytes):");
        for (int i = 0; i < sig.length; i += 16) {
            line(out, String.format("    bytes %2d-%d  %s", i + 1, i + 16, Hex.grouped(sig, i, i + 16)));
        }
        line(out, "    signed again, the same bytes: "
                + Arrays.equals(sig, shop.publish(real).signature()) + "  <- NO RANDOM NUMBER");
        Checkout warehouse = new Checkout(shopPublic(), false);
        line(out, "  CHECKING (the warehouse holds only the public key):");
        line(out, "    public key   " + Hex.grouped(SHOP_PUBLIC, 0, 8) + " ...  (anyone)");
        line(out, "    the real list:                    " + warehouse.check(signed, 50));
        Signed changed = new Signed(list(2, 100, 12, 25), sig);
        line(out, "    laptop changed to 12, same sig:   " + warehouse.check(changed, 50));
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(list(2, 100, 12, 25));
        line(out, "    Mallory's list, signed by her:    " + warehouse.check(forged, 50));
        line(out, "    the warehouse can check, but has no key that signs");
    }

    private static void act3Steps(StringBuilder out) {
        line(out, "THREE. How the signature is made, step by step.");
        byte[] message = Signing.withContext(Publisher.CONTEXT, list(2, 100, 1200, 25).encode());
        SignatureSteps.Steps steps = SignatureSteps.sign(SHOP_SEED, message);
        line(out, "  STEP 1. HASH THE PRIVATE KEY (SHA-512), CUT IN TWO");
        line(out, "    secret a     " + first16(steps.secretA()));
        line(out, "    prefix       " + first16(steps.prefix()));
        line(out, "    A = a x B    " + first16(steps.publicA()));
        line(out, "  STEP 2. r = SHA-512(prefix + message) mod L");
        line(out, "    r            " + first16(steps.r()));
        line(out, "  STEP 3. R = r x B: the first 32 bytes of the signature");
        line(out, "    R            " + first16(steps.bigR()));
        line(out, "  STEP 4. k = SHA-512(R + A + message) mod L");
        line(out, "    k            " + first16(steps.k()));
        line(out, "  STEP 5. S = (r + k x a) mod L: the last 32 bytes");
        line(out, "    S            " + first16(steps.s()));
        line(out, "    R + S is Java's own signature: "
                + Arrays.equals(steps.signature(), Signing.sign(shopPrivate(), Publisher.CONTEXT, list(2, 100, 1200, 25).encode())));
        line(out, "  CHECKING (public key only): S x B = R + k x A");
        line(out, "    the real list:  " + SignatureSteps.checks(SHOP_PUBLIC, message, steps.signature()));
        message[message.length - 2] ^= 1;                 // mouse=25 becomes mouse=24
        line(out, "    mouse=24:       " + SignatureSteps.checks(SHOP_PUBLIC, message, steps.signature()));
    }

    private static void act4Purpose(StringBuilder out) {
        line(out, "FOUR. What exactly is signed.");
        PriceList list = list(2, 100, 1200, 25);
        byte[] signed = Signing.withContext(Publisher.CONTEXT, list.encode());
        int zero = Publisher.CONTEXT.length();
        line(out, "  BEFORE SIGNING (the signed bytes = label + 00 + list):");
        line(out, "    label     " + Publisher.CONTEXT);
        line(out, "    zero byte 00");
        line(out, "    list      " + text(list));
        line(out, "    around 00 " + Hex.of(Arrays.copyOfRange(signed, zero - 4, zero)) + " | 00 | "
                + Hex.of(Arrays.copyOfRange(signed, zero + 1, zero + 5)));
        PriceList sameButBuiltDifferently = new PriceList(2, 100, new java.util.TreeMap<>(Map.of("laptop", 1200, "mouse", 25)));
        line(out, "    built in another order, the same bytes: "
                + Arrays.equals(list.encode(), sameButBuiltDifferently.encode()));
        byte[] signature = Signing.sign(shopPrivate(), "shop.refund.v1", list.encode());
        line(out, "  AFTER SIGNING FOR ANOTHER PURPOSE (label shop.refund.v1):");
        line(out, "    checked as a refund order: " + Signing.verify(shopPublic(), "shop.refund.v1", list.encode(), signature));
        line(out, "    offered as a price list:   "
                + (Signing.verify(shopPublic(), Publisher.CONTEXT, list.encode(), signature) ? "accepted" : "refused")
                + "  <- TIED TO ITS PURPOSE");
    }

    private static void act5Freshness(StringBuilder out) {
        line(out, "FIVE. A good signature on an old message.");
        Publisher shop = new Publisher(shopPrivate());
        PriceList v1 = list(1, 100, 1000, 20);
        PriceList v2 = list(2, 100, 1200, 25);
        Signed old = shop.publish(v1);
        Signed current = shop.publish(v2);
        line(out, "  BEFORE (both genuinely signed by the shop):");
        line(out, "    version 1    " + text(v1));
        line(out, "    version 2    " + text(v2));
        Checkout naive = new Checkout(shopPublic(), false);
        naive.check(current, 50);
        line(out, "  THE ATTACK (version 1 replayed on day 50):");
        line(out, "    signature checked only: " + naive.check(old, 50) + "  <- PERFECT SIGNATURE, OLD LIST");
        Checkout careful = new Checkout(shopPublic(), true);
        careful.check(current, 50);
        line(out, "  AFTER THE FIX (version and expiry are checked):");
        line(out, "    a checkout that has seen version 2: " + careful.check(old, 50));
        Signed shortLived = shop.publish(list(1, 40, 1000, 20));
        line(out, "    valid until day 40, offered on day 50: " + new Checkout(shopPublic(), true).check(shortLived, 50));
        line(out, "    both are inside the signed bytes, so nobody can change them");
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: whose key is it, and what if it is stolen?");
        PriceList forgedList = list(9, 100, 1, 1);
        KeyPair mallory = Ed25519Keys.newKeyPair();
        Signed forged = new Publisher(mallory.getPrivate()).publish(forgedList);
        Checkout fooled = new Checkout(mallory.getPublic(), false);
        line(out, "  THE ATTACK (Mallory's key, said to be the shop's):");
        line(out, "    Mallory's list: " + fooled.check(forged, 50) + "  <- ONLY AS GOOD AS THE KEY");
        Checkout right = new Checkout(shopPublic(), false);
        line(out, "  AFTER THE FIX (the shop's real public key):");
        line(out, "    Mallory's list: " + right.check(forged, 50));
        Signed stolen = new Publisher(shopPrivate()).publish(forgedList);
        line(out, "  WHAT A SIGNATURE CANNOT DO:");
        line(out, "    stop a stolen private key: " + right.check(stolen, 50) + "  <- UNTIL IT IS REVOKED");
        line(out, "    hide anything: the price list is readable by anyone");
        line(out, "    bytes: signature " + Signing.SIGNATURE_BYTES + ", public key " + SHOP_PUBLIC.length
                + ", encoded key " + shopPublic().getEncoded().length);
    }
}
