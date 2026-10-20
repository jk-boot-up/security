package com.jk.explore.security.cors;

/** What a script on a page got: did the request reach the bank's code, and was the page allowed to read the answer? */
public record FetchResult(boolean reachedBank, boolean readable, String body) {

    @Override
    public String toString() {
        return (reachedBank ? "reached the bank" : "never reached the bank") + ", " + (readable ? "readable: " + body : "answer blocked");
    }
}
