package com.jk.explore.security.saml;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Runs the single sign-on story in four numbered acts and prints exact, repeatable results. Keys and ids are random and never printed. */
public class SamlDemo {

    static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String REQUEST = "req-1";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Idp idp = new Idp();
        KeyPair attackerKey = Idp.newKey();

        out.append("== 1. Signing in through the bank ==\n");
        String honest = idp.response(REQUEST, "ana@bank.example", NOW);
        out.append(line("trusting provider, genuine response", new TrustingServiceProvider(), honest));
        out.append(line("strict provider, genuine response", strict(idp), honest));

        out.append("\n== 2. A valid signature is not the same as the bank's signature ==\n");
        String selfSigned = Idp.signedBy(attackerKey, REQUEST, "admin@bank.example", NOW);
        out.append(line("trusting provider, self-signed", new TrustingServiceProvider(), selfSigned));
        out.append(line("pinned-key provider, self-signed", new PinnedKeyServiceProvider(idp.publicKey()), selfSigned));
        String tampered = Attacks.tamper(honest, "admin@bank.example");
        out.append(line("pinned-key provider, edited name", new PinnedKeyServiceProvider(idp.publicKey()), tampered));

        out.append("\n== 3. A valid signature on one assertion, and another one read ==\n");
        String wrapped = Attacks.wrap(honest, "admin@bank.example");
        out.append(line("pinned-key provider, wrapped", new PinnedKeyServiceProvider(idp.publicKey()), wrapped));
        out.append(line("strict provider, wrapped", strict(idp), wrapped));
        out.append(line("strict provider, self-signed", strict(idp), selfSigned));
        out.append(line("strict provider, edited name", strict(idp), tampered));

        out.append("\n== 4. The statement itself is checked ==\n");
        StrictServiceProvider sp = strict(idp);
        out.append(line("genuine response, first time", sp, idp.response(REQUEST, "ana@bank.example", NOW)));
        String again = idp.response(REQUEST, "ana@bank.example", NOW);
        sp.login(again, REQUEST);
        out.append(line("the same response replayed", sp, again));
        out.append(line("expired assertion", strict(idp), idp.responseWith(REQUEST, "ana@bank.example", NOW.minusSeconds(3600), StrictServiceProvider.AUDIENCE, NOW.minusSeconds(3000))));
        out.append(line("meant for another shop", strict(idp), idp.responseWith(REQUEST, "ana@bank.example", NOW, "https://other.example", NOW.plusSeconds(300))));
        out.append(line("answers another request", strict(idp), idp.response("req-999", "ana@bank.example", NOW)));
        out.append(line("response with a DOCTYPE", strict(idp), "<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><r>&x;</r>"));
        return out.toString();
    }

    static StrictServiceProvider strict(Idp idp) {
        return new StrictServiceProvider(idp.publicKey(), CLOCK);
    }

    private static String line(String label, ServiceProvider sp, String xml) {
        String result;
        try {
            result = "signed in as " + sp.login(xml, REQUEST);
        } catch (SamlRefused e) {
            result = "refused: " + e.getMessage();
        }
        return String.format("%-38s -> %s%n", label, result);
    }
}
