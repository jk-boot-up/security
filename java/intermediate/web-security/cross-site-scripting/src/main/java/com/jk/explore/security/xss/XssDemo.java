package com.jk.explore.security.xss;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Runs the online store story in four numbered acts and prints exact, repeatable results. Each act shows its input and its
 * result in separate, labelled sections. Nonces are random, so the nonce itself is printed as {@code <nonce>}.
 */
public class XssDemo {

    static final Review MARKUP = new Review("Ana", "true to size", "<b>Great boots</b>");
    static final Review SCRIPT = new Review("Eve", "runs small", "<script>steal(document.cookie)</script>");
    static final Review QUOTE = new Review("Ben", "Fits 5\" boots", "Warm & dry, price < 20 & > 10");
    static final String ATTACK_FIT_NOTE = "x\" onclick=\"steal()";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Problem(out);
        act2Encoding(out);
        act3Filters(out);
        act4Policy(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The review's part of a page, one element per line, indented under a section header. */
    private static void fragment(StringBuilder out, String page) {
        String div = page.substring(page.indexOf("<div"), page.lastIndexOf("</div>") + 6);
        div = div.replace("<p>", "\n  <p>").replace("<span>", "\n  <span>").replace("<a ", "\n  <a ");
        for (String ln : div.split("\n")) {
            line(out, "    " + ln);
        }
    }

    private static String names(String page) {
        return Markup.elements(page).stream().map(Markup.Element::name).collect(Collectors.joining(" "));
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start) + start.length();
        return text.substring(from, text.indexOf(end, from));
    }

    private static void act1Problem(StringBuilder out) {
        line(out, "ONE. A review that changes the page.");
        String plain = ReviewPage.unsafe(List.of(new Review("Ana", MARKUP.fitNote(), "Great boots")));
        String page = ReviewPage.unsafe(List.of(MARKUP));
        line(out, "  THE REVIEW (typed by Ana):");
        line(out, "    " + MARKUP.text());
        line(out, "  THE PAGE AS BUILT (the review pasted in as it is):");
        fragment(out, page);
        line(out, "  THE RESULT (the elements a browser finds):");
        line(out, "    plain text    " + names(plain));
        line(out, "    Ana's review  " + names(page));
        line(out, "    " + (Markup.count(page, "b") - Markup.count(plain, "b"))
                + " b element the shop never wrote  <- THE PAGE CHANGED");
        String eves = ReviewPage.unsafe(List.of(SCRIPT));
        line(out, "  THE REVIEW (typed by Eve):");
        line(out, "    " + SCRIPT.text());
        line(out, "  THE RESULT:");
        line(out, "    script elements: " + Markup.count(eves, "script") + "  <- IT RUNS AS THE SHOP'S OWN CODE");
        line(out, "    it could act as the signed-in customer");
    }

    private static void act2Encoding(StringBuilder out) {
        line(out, "TWO. Encoding for the place the text goes.");
        String[][] places = {
            {"between tags    ", MARKUP.text(), Encode.htmlBody(MARKUP.text())},
            {"quoted attribute", QUOTE.fitNote(), Encode.attribute(QUOTE.fitNote())},
            {"URL parameter   ", "size 5 & up", Encode.urlParameter("size 5 & up")},
            {"script string   ", "It's \"great\"", Encode.javaScriptString("It's \"great\"")},
        };
        line(out, "  BEFORE ENCODING (the place, and the customer's text):");
        for (String[] p : places) {
            line(out, "    " + p[0] + "  " + p[1]);
        }
        line(out, "  AFTER ENCODING:");
        for (String[] p : places) {
            line(out, "    " + p[0] + "  " + p[2]);
        }
        line(out, "  HOW EACH CHARACTER IS ENCODED:");
        line(out, "    char   HTML    URL  script");
        for (String c : List.of("<", ">", "&", "\"", "'", " ")) {
            line(out, String.format(Locale.ROOT, "    %-6s %-7s %-4s %s", shown(c), shown(Encode.htmlBody(c)),
                    shown(Encode.urlParameter(c)), shown(Encode.javaScriptString(c))));
        }
        int code = '"';
        line(out, "    \" is character " + code + " = " + code / 16 + " x 16 + " + code % 16 + ", so "
                + HexFormat.of().toHexDigits((byte) code) + " in hex");
        String bens = ReviewPage.safe(List.of(QUOTE), "fixed-for-this-act");
        line(out, "  THE ENCODED PAGE (Ben's review):");
        fragment(out, bens);
        line(out, "  THE RESULT (what the browser reads back):");
        line(out, "    fit note     " + Markup.attribute(bens, "div", "data-fit"));
        line(out, "    review text  " + Encode.decodeHtml(between(bens, "<p>", "</p>")));
        line(out, "    Ana's review, encoded: b elements "
                + Markup.count(ReviewPage.safe(List.of(MARKUP), "fixed-for-this-act"), "b"));
    }

    private static String shown(String s) {
        return s.equals(" ") ? "space" : s;
    }

    private static void act3Filters(StringBuilder out) {
        line(out, "THREE. Why filtering input is not the fix.");
        line(out, "  BEFORE THE FILTER (Ben's review, Eve's fit note):");
        line(out, "    " + QUOTE.text());
        line(out, "    " + ATTACK_FIT_NOTE);
        line(out, "  AFTER A FILTER THAT DELETES < AND >:");
        line(out, "    " + NaiveFilter.clean(QUOTE.text()) + "  <- HONEST WORDS CHANGED");
        line(out, "    " + NaiveFilter.clean(ATTACK_FIT_NOTE) + "  <- THE ATTACK IS UNTOUCHED");
        String filtered = ReviewPage.unsafe(List.of(new Review("Eve", NaiveFilter.clean(ATTACK_FIT_NOTE), "ok")));
        line(out, "  THE PAGE AS BUILT (the filtered fit note pasted in):");
        line(out, "    " + between(filtered, "</h1>", "<p>"));
        line(out, "  THE RESULT (the attributes a browser finds on the div):");
        line(out, "    " + String.join("  ", Markup.attributes(filtered, "div")));
        line(out, "    <- A SCRIPT RUNS WHEN THE REVIEW IS CLICKED");
        String encoded = ReviewPage.safe(List.of(new Review("Eve", ATTACK_FIT_NOTE, "ok")), "fixed-for-this-act");
        line(out, "  ENCODED INSTEAD (the div's data-fit attribute):");
        line(out, "    data-fit=" + between(encoded, "data-fit=", "><p>"));
        line(out, "  THE RESULT:");
        line(out, "    " + String.join("  ", Markup.attributes(encoded, "div")));
        line(out, "    Ben's words read back exactly: " + Encode.decodeHtml(Encode.htmlBody(QUOTE.text())).equals(QUOTE.text()));
        line(out, "  a filter guesses; an encoder is chosen for the place");
    }

    private static void act4Policy(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. A strict Content-Security-Policy, with a fresh nonce.");
        try (ShopServer shop = new ShopServer(List.of(MARKUP, QUOTE))) {
            HttpResponse<String> first = shop.get();
            HttpResponse<String> second = shop.get();
            String policy = first.headers().firstValue("Content-Security-Policy").orElseThrow();
            String nonce = Markup.scriptNonces(first.body()).get(0);
            String secondPolicy = second.headers().firstValue("Content-Security-Policy").orElse("");
            String cookie = first.headers().firstValue("Set-Cookie").orElse("");
            line(out, "  THE REQUEST (to the shop, served for real on this machine):");
            line(out, "    GET /product");
            line(out, "  THE RESPONSE (its nonce is shown as <nonce>):");
            line(out, "    status " + first.statusCode() + ", and the header Content-Security-Policy:");
            for (String directive : policy.replace(nonce, "<nonce>").split("; ")) {
                line(out, "      " + directive);
            }
            int cut = cookie.indexOf("Path=/;") + 7;
            line(out, "    Set-Cookie: " + cookie.substring(0, cut));
            line(out, "      " + cookie.substring(cut).strip());
            String script = first.body().substring(first.body().indexOf("<script"), first.body().indexOf("</script>") + 9);
            line(out, "    " + script.replace(nonce, "<nonce>"));
            line(out, "  HOW THE NONCE IS MADE:");
            int bytes = ContentSecurityPolicy.NONCE_BYTES;
            int groups = (bytes + 2) / 3;
            line(out, "    " + bytes + " random bytes = " + bytes * 8 + " bits, written in Base64");
            line(out, "    Base64 turns every 3 bytes into 4 characters");
            line(out, "    " + bytes + " bytes -> " + groups + " groups (the last one short) -> " + groups * 4 + " characters");
            byte[] example = new byte[bytes];
            for (int i = 0; i < bytes; i++) {
                example[i] = (byte) i;
            }
            line(out, "    example, bytes 00 01 02 ... 0f -> " + Base64.getEncoder().encodeToString(example));
            line(out, "    this page's nonce: " + nonce.length() + " characters; the next page's differs: "
                    + !secondPolicy.contains(nonce));
            line(out, "    the script's nonce is the policy's nonce: " + policy.contains("'nonce-" + nonce + "'"));
            line(out, "  WHICH SCRIPTS MAY RUN (what a browser decides):");
            line(out, "    the shop's script, with the nonce     " + yes(ContentSecurityPolicy.allowsInlineScript(policy, nonce)));
            line(out, "    Eve's script, without a nonce         " + yes(ContentSecurityPolicy.allowsInlineScript(policy, null))
                    + "  <- BLOCKED");
            line(out, "    the same script, with no policy       " + yes(ContentSecurityPolicy.allowsInlineScript(null, null)));
            line(out, "    'unsafe-inline' and no nonce          "
                    + yes(ContentSecurityPolicy.allowsInlineScript("script-src 'self' 'unsafe-inline'", null)) + "  <- PROTECTS NOTHING");
            line(out, "    the cookie is HttpOnly, so no script can read it: "
                    + cookie.toLowerCase(Locale.ROOT).contains("httponly"));
        }
    }

    private static String yes(boolean allowed) {
        return allowed ? "yes" : "no";
    }
}
