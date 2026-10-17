package com.jk.explore.security.csrf;

/**
 * Which defences the bank switches on.
 *
 * @param tokens       a per-session token that every state-changing form must carry
 * @param sameSite     the SameSite attribute of the session cookie: null (none set), "Lax", "Strict" or "None"
 * @param fetchChecks  refuse state-changing requests whose Sec-Fetch-Site or Origin shows another origin
 * @param getTransfers whether a GET request may move money (it never should)
 */
record Protection(boolean tokens, String sameSite, boolean fetchChecks, boolean getTransfers) {

    static Protection none() {
        return new Protection(false, "None", false, false);
    }

    Protection withTokens() {
        return new Protection(true, sameSite, fetchChecks, getTransfers);
    }

    Protection withSameSite(String value) {
        return new Protection(tokens, value, fetchChecks, getTransfers);
    }

    Protection withFetchChecks() {
        return new Protection(tokens, sameSite, true, getTransfers);
    }

    Protection withGetTransfers() {
        return new Protection(tokens, sameSite, fetchChecks, true);
    }
}
