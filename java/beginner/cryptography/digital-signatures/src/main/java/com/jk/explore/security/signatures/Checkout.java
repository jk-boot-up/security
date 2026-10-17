package com.jk.explore.security.signatures;

import java.security.PublicKey;

/**
 * A service that must only use price lists published by the shop. It holds the shop's public key, which is enough to check a
 * signature and not enough to make one. It can also insist on freshness: a list that has expired, or that is older than one it
 * already accepted, is refused even if its signature is perfect.
 */
public class Checkout {

    private final PublicKey shopKey;
    private final boolean checkFreshness;
    private int newestVersion;

    public Checkout(PublicKey shopKey, boolean checkFreshness) {
        this.shopKey = shopKey;
        this.checkFreshness = checkFreshness;
    }

    /** Checks a price list on a given day (a number of days). */
    public Verdict check(Signed signed, int today) {
        if (!Signing.verify(shopKey, Publisher.CONTEXT, signed.priceList().encode(), signed.signature())) {
            return Verdict.BAD_SIGNATURE;
        }
        if (checkFreshness) {
            if (today > signed.priceList().validUntilDay()) {
                return Verdict.EXPIRED;
            }
            if (signed.priceList().version() < newestVersion) {
                return Verdict.OLD_VERSION;
            }
        }
        newestVersion = Math.max(newestVersion, signed.priceList().version());
        return Verdict.OK;
    }
}
