package com.jk.explore.security.xss;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Nonces are random, so only facts about them are printed. */
public class XssDemo {

    static final Review MARKUP = new Review("Ana", "true to size", "<b>Great boots</b>");
    static final Review QUOTE = new Review("Ben", "Fits 5\" boots", "Warm & dry, price < 20 & > 10");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Problem(out);
        act2Encoding(out);
        act3Filters(out);
        act4Policy(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Problem(StringBuilder out) {
        line(out, "ONE. A review that changes the page.");
        String page = ReviewPage.unsafe(List.of(MARKUP));
        line(out, "  Ana's review is the text <b>Great boots</b>, pasted into the page as it is");
        line(out, "  bold elements on the page: " + Markup.count(page, "b") + ", and the shop wrote none  <- THE REVIEW BECAME PART OF THE PAGE");
        line(out, "  the browser cannot tell the shop's markup from the customer's, so a script in a review would run as the shop's own");
        line(out, "  that script could read the page, act as the signed-in customer, and send what it finds elsewhere");
    }

    private static void act2Encoding(StringBuilder out) {
        line(out, "TWO. Encoding for the place the text goes.");
        line(out, "  between tags: " + Encode.htmlBody(MARKUP.text()));
        line(out, "  in a quoted attribute: " + Encode.attribute(QUOTE.fitNote()));
        line(out, "  in a URL parameter: " + Encode.urlParameter("size 5 & up"));
        line(out, "  in a script string: " + Encode.javaScriptString("It's \"great\""));
        String page = ReviewPage.safe(List.of(MARKUP), "fixed-for-this-act");
        String bens = ReviewPage.safe(List.of(QUOTE), "fixed-for-this-act");
        line(out, "  the encoded page: bold elements " + Markup.count(page, "b") + ", and Ben's fit note reads back whole: "
                + Markup.attribute(bens, "div", "data-fit"));
        line(out, "  every value shows as the text the customer typed, and none of it is read as markup");
    }

    private static void act3Filters(StringBuilder out) {
        line(out, "THREE. Why filtering input is not the fix.");
        String honest = QUOTE.text();
        line(out, "  an honest review: " + honest);
        line(out, "  after a filter that deletes < and >: " + NaiveFilter.clean(honest) + "  <- THE CUSTOMER'S WORDS ARE CHANGED");
        line(out, "  encoded instead, the page shows it exactly: "
                + Encode.decodeHtml(Encode.htmlBody(honest)).equals(honest));
        String filtered = ReviewPage.unsafe(List.of(new Review("Ben", NaiveFilter.clean(QUOTE.fitNote()), "ok")));
        line(out, "  the filter keeps the quote in Ben's fit note, so the attribute ends early and reads: "
                + Markup.attribute(filtered, "div", "data-fit") + "  <- THE FILTER DID NOT KNOW THE CONTEXT");
        line(out, "  a filter guesses at every place text will go; an encoder is chosen for the one place it does go");
    }

    private static void act4Policy(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. A strict Content-Security-Policy, with a fresh nonce for every page.");
        try (ShopServer shop = new ShopServer(List.of(MARKUP, QUOTE))) {
            HttpResponse<String> first = shop.get();
            HttpResponse<String> second = shop.get();
            String policy = first.headers().firstValue("Content-Security-Policy").orElse(null);
            String nonce = Markup.scriptNonces(first.body()).get(0);
            String secondPolicy = second.headers().firstValue("Content-Security-Policy").orElse("");
            line(out, "  the product page answers " + first.statusCode() + " with a policy: " + (policy != null));
            line(out, "  the policy names a nonce, and the shop's own script carries it: " + policy.contains("'nonce-" + nonce + "'"));
            line(out, "  the nonce is " + nonce.length() + " characters of random text, and the next page has a different one: "
                    + !secondPolicy.contains(nonce));
            line(out, "  the shop's script may run: " + ContentSecurityPolicy.allowsInlineScript(policy, nonce));
            line(out, "  a script that slipped in without the nonce may run: " + ContentSecurityPolicy.allowsInlineScript(policy, null)
                    + "  <- A SECOND LINE OF DEFENCE");
            line(out, "  the same script with no policy at all: " + ContentSecurityPolicy.allowsInlineScript(null, null));
            line(out, "  a policy with 'unsafe-inline' and no nonce: " + ContentSecurityPolicy.allowsInlineScript("script-src 'self' 'unsafe-inline'", null)
                    + ", so it protects nothing");
            String cookie = first.headers().firstValue("Set-Cookie").orElse("").toLowerCase(Locale.ROOT);
            line(out, "  the session cookie is HttpOnly, so a script cannot read it: " + cookie.contains("httponly"));
        }
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        line(out, "  this one page writes untrusted text into 4 kinds of place, and each needs its own encoder; one forgotten place is enough");
        line(out, "  the policy needs care: every inline script must carry the nonce, and every new source must be added on purpose");
        line(out, "  the nonce must be new for every response, or a page from yesterday hands an attacker today's key");
        line(out, "  in practice, a template engine that encodes by default does the first part: see the tools version of this project");
    }
}
