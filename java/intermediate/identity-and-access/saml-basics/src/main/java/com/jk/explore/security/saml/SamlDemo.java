package com.jk.explore.security.saml;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Runs the single sign-on story in five acts, each split into labelled sections (the input, then the result), and prints exact, repeatable results.
 * Assertion ids and the clock are fixed, so digests are the same on every run; the RSA keys are new on every run, so signature values are placeholders.
 */
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
        String honest = idp.response(REQUEST, "ana@bank.example", NOW); // assertion a1

        out.append("ONE. Signing in through the bank.\n");
        section(out, "THE REQUEST (the shop sends Ana's browser to the bank):");
        item(out, "AuthnRequest  ID " + REQUEST + ", from " + StrictServiceProvider.AUDIENCE);
        item(out, "reply to      " + StrictServiceProvider.ACS);
        section(out, "THE RESPONSE (the bank's signed assertion, what matters):");
        Element a = assertion(honest);
        item(out, "Assertion ID  " + a.getAttribute("ID") + ", issued " + a.getAttribute("IssueInstant"));
        item(out, "Issuer        " + Xml.text(a, Xml.SAML, "Issuer") + "  (who says so)");
        item(out, "NameID        " + Xml.text(a, Xml.SAML, "NameID") + "  (who signed in)");
        Element conf = (Element) a.getElementsByTagNameNS(Xml.SAML, "SubjectConfirmationData").item(0);
        Element cond = (Element) a.getElementsByTagNameNS(Xml.SAML, "Conditions").item(0);
        item(out, "InResponseTo  " + conf.getAttribute("InResponseTo") + "  (answers this request)");
        item(out, "Recipient     " + conf.getAttribute("Recipient"));
        item(out, "Audience      " + Xml.text(cond, Xml.SAML, "Audience") + "  (for this shop)");
        item(out, "NotBefore     " + cond.getAttribute("NotBefore") + "  = issued - 60 s");
        item(out, "NotOnOrAfter  " + cond.getAttribute("NotOnOrAfter") + "  = issued + 300 s");
        item(out, "Signature     inside the assertion, covers #" + a.getAttribute("ID"));
        section(out, "THE RESULT:");
        item(out, pad("trusting provider") + result(new TrustingServiceProvider(), honest));
        item(out, pad("strict provider") + result(strict(idp), honest));

        out.append("TWO. How the bank signs the assertion.\n");
        SignatureSteps.Steps s = SignatureSteps.of(honest, idp.publicKey());
        section(out, "BEFORE SIGNING (the assertion, as XML):");
        String c14n = new String(s.canonicalAssertion(), StandardCharsets.UTF_8);
        item(out, cut(c14n, 58));
        item(out, cut(c14n.substring(c14n.indexOf("<saml:Issuer")), 58));
        item(out, "(" + s.canonicalAssertion().length + " bytes once canonicalized)");
        section(out, "HOW THE BANK SIGNS IT (step by step):");
        item(out, "1. drop the Signature element itself (enveloped)");
        item(out, "2. canonicalize: exclusive XML c14n -> " + s.canonicalAssertion().length + " bytes");
        item(out, "3. DigestValue = base64(SHA-256(those bytes))");
        item(out, "   " + s.digestValue());
        item(out, "4. SignedInfo = Reference #" + s.coveredId() + " + DigestValue -> " + s.canonicalSignedInfo().length + " bytes");
        item(out, "5. SignatureValue = RSA-SHA256(bank's private key, SignedInfo)");
        item(out, "   " + s.signatureBytes() + " bytes = <random: a new RSA key on every run>");
        section(out, "AFTER SIGNING (inside the assertion, after the Issuer):");
        item(out, "<ds:Signature> Reference URI #" + s.coveredId() + ", DigestValue, SignatureValue,");
        item(out, "               KeyInfo: the signer's RSA public key");
        section(out, "HOW THE SHOP CHECKS IT (redone by hand, with the bank's key):");
        item(out, "1. SHA-256 of the same bytes again = DigestValue: " + s.digestMatches());
        item(out, "2. RSA verify of SignedInfo, the bank's public key: " + s.signatureByHand());
        item(out, "3. the library's XMLSignature.validate agrees: " + (s.library() == (s.digestMatches() && s.signatureByHand())));
        section(out, "THE RESULT:");
        item(out, "the signature covers #" + s.coveredId() + " and nothing else");

        out.append("THREE. A valid signature is not the same as the bank's signature.\n");
        String selfSigned = Idp.signedBy(attackerKey, REQUEST, "admin@bank.example", NOW);
        section(out, "THE ATTACKER'S RESPONSE (same XML, the attacker's own key):");
        item(out, "NameID   " + Xml.text(assertion(selfSigned), Xml.SAML, "NameID"));
        item(out, "KeyInfo  the attacker's public key, inside the message");
        section(out, "BEFORE THE FIX (trusting provider: the key in KeyInfo):");
        item(out, "signature valid with the key it carries: "
                + SignatureSteps.of(selfSigned, attackerKey.getPublic()).library());
        item(out, "-> " + result(new TrustingServiceProvider(), selfSigned));
        item(out, "<- ANYONE CAN SIGN");
        section(out, "AFTER THE FIX (pinned-key provider: the bank's key only):");
        item(out, "signature valid with the bank's key: " + SignatureSteps.of(selfSigned, idp.publicKey()).library());
        item(out, "-> " + result(new PinnedKeyServiceProvider(idp.publicKey()), selfSigned));
        String tampered = Attacks.tamper(honest, "admin@bank.example");
        SignatureSteps.Steps t = SignatureSteps.of(tampered, idp.publicKey());
        section(out, "ONE NAME EDITED IN THE GENUINE RESPONSE (the input):");
        item(out, "NameID ana@bank.example -> " + Xml.text(assertion(tampered), Xml.SAML, "NameID"));
        section(out, "THE DIGEST, AS SIGNED AND AFTER THE EDIT:");
        item(out, "DigestValue  " + t.digestValue());
        item(out, "SHA-256 now  " + t.digestByHand());
        section(out, "THE RESULT:");
        item(out, "-> " + result(new PinnedKeyServiceProvider(idp.publicKey()), tampered));

        out.append("FOUR. Signature wrapping: one assertion checked, another read.\n");
        String wrapped = Attacks.wrap(honest, "admin@bank.example");
        section(out, "THE WRAPPED RESPONSE (the attacker moves the signed one):");
        tree(out, Xml.parse(wrapped).getDocumentElement(), "");
        section(out, "BEFORE THE FIX (pinned-key provider):");
        SignatureSteps.Steps w = SignatureSteps.of(wrapped, idp.publicKey());
        item(out, "1. first signature, the bank's key: valid " + w.library() + ", covers #" + w.coveredId());
        Element first = assertion(wrapped);
        item(out, "2. first assertion read: ID " + first.getAttribute("ID") + ", NameID " + Xml.text(first, Xml.SAML, "NameID"));
        item(out, "-> " + result(new PinnedKeyServiceProvider(idp.publicKey()), wrapped));
        item(out, "<- CHECKED ONE ELEMENT, READ ANOTHER");
        section(out, "AFTER THE FIX (strict provider: read only what is signed):");
        item(out, "assertions in the document: " + Xml.byName(Xml.parse(wrapped), Xml.SAML, "Assertion").getLength());
        item(out, "-> " + result(strict(idp), wrapped));
        section(out, "THE STRICT PROVIDER AND THE OTHER TWO ATTACKS:");
        item(out, pad("self-signed") + result(strict(idp), selfSigned));
        item(out, pad("edited name") + result(strict(idp), tampered));

        out.append("FIVE. The statement itself is checked.\n");
        StrictServiceProvider sp = strict(idp);
        String fresh = idp.response(REQUEST, "ana@bank.example", NOW); // assertion a2
        Element f = assertion(fresh);
        Element fc = (Element) f.getElementsByTagNameNS(Xml.SAML, "Conditions").item(0);
        section(out, "THE STRICT PROVIDER'S CHECKS (now " + NOW + "):");
        item(out, "issuer   " + Xml.text(f, Xml.SAML, "Issuer") + ": " + Idp.ISSUER.equals(Xml.text(f, Xml.SAML, "Issuer")));
        item(out, "time     NotBefore - 60 s <= now < NotOnOrAfter + 60 s: "
                + (!NOW.plusSeconds(60).isBefore(Instant.parse(fc.getAttribute("NotBefore")))
                        && NOW.minusSeconds(60).isBefore(Instant.parse(fc.getAttribute("NotOnOrAfter")))));
        String audience = Xml.text(fc, Xml.SAML, "Audience");
        item(out, "audience " + audience + ": " + StrictServiceProvider.AUDIENCE.equals(audience));
        String answers = ((Element) f.getElementsByTagNameNS(Xml.SAML, "SubjectConfirmationData").item(0)).getAttribute("InResponseTo");
        item(out, "request  InResponseTo " + answers + " is the one this browser made: " + REQUEST.equals(answers));
        item(out, "replay   assertion id " + f.getAttribute("ID") + " not seen before: true");
        section(out, "THE RESULT:");
        item(out, pad("genuine response, first time") + result(sp, fresh));
        section(out, "ONE THING CHANGED AT A TIME (the input, then the result):");
        item(out, "the same response again (assertion " + f.getAttribute("ID") + ")");
        item(out, "  -> " + result(sp, fresh));
        String expired = idp.responseWith(REQUEST, "ana@bank.example", NOW.minusSeconds(3600), StrictServiceProvider.AUDIENCE, NOW.minusSeconds(3000));
        item(out, "NotOnOrAfter " + condition(expired, "NotOnOrAfter") + " (50 minutes ago)");
        item(out, "  -> " + result(strict(idp), expired));
        String other = idp.responseWith(REQUEST, "ana@bank.example", NOW, "https://other.example", NOW.plusSeconds(300));
        item(out, "Audience " + Xml.text(assertion(other), Xml.SAML, "Audience") + " (another shop)");
        item(out, "  -> " + result(strict(idp), other));
        String otherRequest = idp.response("req-999", "ana@bank.example", NOW);
        item(out, "InResponseTo req-999 (a request this browser never made)");
        item(out, "  -> " + result(strict(idp), otherRequest));
        item(out, "<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>");
        item(out, "  -> " + result(strict(idp), "<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><r>&x;</r>"));
        return out.toString();
    }

    static StrictServiceProvider strict(Idp idp) {
        return new StrictServiceProvider(idp.publicKey(), CLOCK);
    }

    private static String result(ServiceProvider sp, String xml) {
        try {
            return "signed in as " + sp.login(xml, REQUEST);
        } catch (SamlRefused e) {
            return "refused: " + e.getMessage();
        }
    }

    private static Element assertion(String xml) {
        return (Element) Xml.byName(Xml.parse(xml), Xml.SAML, "Assertion").item(0);
    }

    private static String condition(String xml, String attribute) {
        return ((Element) assertion(xml).getElementsByTagNameNS(Xml.SAML, "Conditions").item(0)).getAttribute(attribute);
    }

    /** The document's outline: Response, Assertions (id and name), Extensions and Signatures, indented by depth. */
    private static void tree(StringBuilder out, Element e, String indent) {
        String name = e.getTagName();
        String line = switch (e.getLocalName()) {
            case "Response", "Extensions" -> name;
            case "Assertion" -> name + " ID " + e.getAttribute("ID") + ", NameID " + Xml.text(e, Xml.SAML, "NameID")
                    + (e.getElementsByTagNameNS(Xml.DS, "Signature").getLength() == 0 ? ", unsigned" : "");
            case "Signature" -> "ds:Signature" + " covers #" + ((Element) e.getElementsByTagNameNS(Xml.DS, "Reference").item(0)).getAttribute("URI").substring(1);
            default -> null;
        };
        if (line != null) {
            item(out, indent + line);
        }
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child) {
                tree(out, child, line == null ? indent : indent + "  ");
            }
        }
    }

    private static void section(StringBuilder out, String header) {
        out.append("  ").append(header).append('\n');
    }

    private static void item(StringBuilder out, String text) {
        out.append("    ").append(text).append('\n');
    }

    private static String pad(String label) {
        return String.format("%-30s -> ", label);
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
