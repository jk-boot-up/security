package com.jk.explore.security.samlkeycloak;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Runs the shop's single sign-on story in five acts, each split into labelled sections (the input, then the result), against a real Keycloak in Docker and a real Spring Security SAML 2.0 service provider. */
public class SamlKeycloakDemo {

    private static final Map<String, String> FORM = Map.of("Content-Type", "application/x-www-form-urlencoded");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Ports, ids, keys and signatures are never printed. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        int port = Running.freePort();
        try (KeycloakLab keycloak = KeycloakLab.start(port)) {
            try (Running shop = Running.start(keycloak, port, "bank")) {
                Refusals refusals = shop.context().getBean(Refusals.class);
                // One sign-in that is not printed: the shop's first response loads OpenSAML, which can take longer than an assertion's two seconds.
                Stopped warmUp = signedInAtTheBank(shop, "ana");
                post(shop, warmUp, warmUp.response(), refusals);
                act1SignIn(out, keycloak, shop);
                act2WrongSigner(out, keycloak, shop, refusals);
                act3Wrapping(out, shop, refusals);
                act4Statement(out, keycloak, shop, refusals);
            }
            act5Bill(out, keycloak, port);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A browser that has signed in at the bank and been stopped just before it posts the bank's response to the shop. */
    record Stopped(Shopper browser, Map<String, String> fields, String requestId) {

        Document response() {
            return SamlTools.parse(SamlTools.decode(fields.get("SAMLResponse")));
        }
    }

    static Stopped signedInAtTheBank(Running shop, String user) throws IOException, InterruptedException {
        Shopper browser = new Shopper();
        Shopper.Page page = browser.navigate(shop.url() + "/saml2/authenticate/keycloak", user, "demo-password", shop.url() + Running.ACS_PATH);
        Map<String, String> fields = Shopper.hiddenFields(page.body());
        Document response = SamlTools.parse(SamlTools.decode(fields.get("SAMLResponse")));
        return new Stopped(browser, fields, SamlTools.response(response).getAttribute("InResponseTo"));
    }

    /** The browser posts these XML bytes to the shop, as the bank's response, with the relay state it was given. */
    static String post(Running shop, Stopped s, Document xml, Refusals refusals) throws IOException, InterruptedException {
        return post(shop, s.browser(), s.fields(), SamlTools.serialize(xml), refusals);
    }

    static String post(Running shop, Shopper browser, Map<String, String> fields, String xml, Refusals refusals) throws IOException, InterruptedException {
        Map<String, String> f = new LinkedHashMap<>(fields);
        f.put("SAMLResponse", SamlTools.encode(xml));
        int before = refusals.all().size();
        Shopper.Reply r = browser.request("POST", shop.url() + Running.ACS_PATH, FORM, Shopper.formBody(f), false);
        if (refusals.all().size() > before) {
            return "refused (" + refusals.last() + ")";
        }
        return r.status() / 100 == 3 && browser.get(shop.url() + "/me").status() == 200 ? "signed in as " + who(browser, shop) : "refused (status " + r.status() + ")";
    }

    static String who(Shopper browser, Running shop) {
        try {
            Matcher m = Pattern.compile("\"name\":\"([^\"]+)\"").matcher(browser.get(shop.url() + "/me").body());
            return m.find() ? m.group(1) : "nobody";
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void act1SignIn(StringBuilder out, KeycloakLab keycloak, Running shop) throws IOException, InterruptedException {
        line(out, "ONE. Signing in through the bank.");
        Shopper browser = new Shopper();
        Shopper.Page toBank = browser.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = Shopper.hiddenFields(toBank.body());
        Element authn = SamlTools.parse(SamlTools.decode(request.get("SAMLRequest"))).getDocumentElement();
        section(out, "THE REQUEST (the shop sends Ana's browser to the bank):");
        item(out, "shop          http://127.0.0.1:<port>");
        item(out, "AuthnRequest  ID <random>, signed: "
                + (authn.getElementsByTagNameNS(SamlTools.DSIG, "Signature").getLength() > 0));
        item(out, "Issuer        " + shopPath(shop, issuerOf(authn)));
        item(out, "answer at     " + shopPath(shop, authn.getAttribute("AssertionConsumerServiceURL")));

        Stopped s = signedInAtTheBank(shop, "ana");
        Document response = s.response();
        Element root = SamlTools.response(response);
        Element assertion = SamlTools.firstAssertion(response);
        Element conditions = (Element) assertion.getElementsByTagNameNS(SamlTools.SAML, "Conditions").item(0);
        // posted at once: the assertion lasts two seconds; the steps below are printed afterwards
        Refusals refusals = shop.context().getBean(Refusals.class);
        String result = post(shop, s, response, refusals);
        String me = s.browser().get(shop.url() + "/me").body();
        long lifetime = java.time.Duration.between(java.time.Instant.parse(conditions.getAttribute("NotBefore")),
                java.time.Instant.parse(conditions.getAttribute("NotOnOrAfter"))).toSeconds();
        section(out, "THE RESPONSE (Ana signs in at the bank, never at the shop):");
        item(out, "Issuer        " + issuerOf(root).replace(keycloak.base(), "<keycloak>") + "  (the realm)");
        item(out, "InResponseTo  the shop's request ID: " + s.requestId().startsWith("ARQ"));
        item(out, "Destination   " + shopPath(shop, root.getAttribute("Destination")));
        item(out, "NameID        " + SamlTools.nameId(response) + "  (who signed in)");
        item(out, "Audience      " + shopPath(shop, text(assertion, "Audience")));
        item(out, "lifetime      NotOnOrAfter - NotBefore = " + lifetime + " seconds");
        int signatures = response.getElementsByTagNameNS(SamlTools.DSIG, "Signature").getLength();
        item(out, "signatures    " + signatures + ": the Response, and the Assertion on its own");

        String metadata = s.browser().get(keycloak.metadata("bank")).body();
        java.security.PublicKey bankKey = SignatureByHand.keyFromMetadata(metadata);
        Element signedInfo = (Element) response.getElementsByTagNameNS(SamlTools.DSIG, "SignedInfo").item(0);
        section(out, "HOW THE SIGNATURES ARE CHECKED (redone by hand):");
        item(out, "the bank's key, from its metadata: " + bankKey.getAlgorithm() + ", "
                + ((java.security.interfaces.RSAPublicKey) bankKey).getModulus().bitLength() + " bits");
        item(out, "SignatureMethod " + algorithm(signedInfo, "SignatureMethod") + ", DigestMethod "
                + algorithm(signedInfo, "DigestMethod"));
        item(out, "CanonicalizationMethod " + algorithm(signedInfo, "CanonicalizationMethod"));
        for (SignatureByHand.Check c : SignatureByHand.checkAll(SamlTools.parse(SamlTools.serialize(response)), bankKey)) {
            item(out, String.format("%-10s SHA-256 = DigestValue: %s, RSA verify: %s", c.covers() + ":", c.digestMatches(), c.rsaVerifies()));
        }
        section(out, "THE RESULT:");
        item(out, "the browser posts it to the shop -> " + result);
        item(out, "the shop's page /me: name " + jsonField(me, "name") + ", email " + jsonField(me, "email"));
    }

    private static void act2WrongSigner(StringBuilder out, KeycloakLab keycloak, Running shop, Refusals refusals) throws IOException, InterruptedException {
        line(out, "TWO. A valid signature is not the same as the bank's signature.");
        java.security.PublicKey bankKey = SignatureByHand.keyFromMetadata(new Shopper().get(keycloak.metadata("bank")).body());
        Stopped edited = signedInAtTheBank(shop, "ana");
        Document doc = edited.response();
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        section(out, "ONE NAME EDITED IN THE BANK'S RESPONSE (the input):");
        item(out, "NameID ana -> admin, both signatures left in place");
        for (SignatureByHand.Check c : SignatureByHand.checkAll(SamlTools.parse(SamlTools.serialize(doc)), bankKey)) {
            item(out, String.format("%-10s SHA-256 = DigestValue: %s", c.covers() + ":", c.digestMatches()));
        }
        section(out, "THE RESULT:");
        item(out, "-> " + post(shop, edited, doc, refusals));

        Stopped selfSigned = signedInAtTheBank(shop, "ana");
        doc = selfSigned.response();
        SamlTools.stripSignatures(doc);
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        java.security.KeyPair attacker = SamlTools.attackerKey();
        SamlTools.sign(SamlTools.firstAssertion(doc), attacker);
        SamlTools.sign(SamlTools.response(doc), attacker);
        Document reparsed = SamlTools.parse(SamlTools.serialize(doc));
        section(out, "THE ATTACKER'S RESPONSE (admin, signed with the attacker's key):");
        item(out, SamlTools.describeSignatures(doc) + ", KeyInfo: the attacker's public key");
        item(out, "RSA verify with the attacker's key: " + verdicts(SignatureByHand.checkAll(reparsed, attacker.getPublic())));
        item(out, "RSA verify with the bank's key:     " + verdicts(SignatureByHand.checkAll(reparsed, bankKey)));
        section(out, "THE RESULT:");
        item(out, "-> " + post(shop, selfSigned, doc, refusals));
        item(out, "<- THE KEY IN THE MESSAGE IS NEVER TRUSTED");

        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        String requestId = SamlTools.parse(SamlTools.decode(request.get("SAMLRequest"))).getDocumentElement().getAttribute("ID");
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replace(keycloak.issuer("bank") + "/protocol/saml", keycloak.issuer("wallet") + "/protocol/saml")));
        Shopper.Page walletPage = new Shopper().navigatePost(keycloak.issuer("wallet") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH);
        Map<String, String> walletFields = Shopper.hiddenFields(walletPage.body());
        Document wallet = SamlTools.parse(SamlTools.decode(walletFields.get("SAMLResponse")));
        section(out, "A GENUINE RESPONSE FROM THE WALLET (another identity provider):");
        item(out, "Issuer        " + issuerOf(SamlTools.response(wallet)).replace(keycloak.base(), "<keycloak>"));
        item(out, "InResponseTo  the shop's request ID: " + SamlTools.response(wallet).getAttribute("InResponseTo").equals(requestId));
        item(out, "signed by the wallet's own key, valid XML");
        section(out, "THE RESULT:");
        item(out, "-> " + post(shop, b, walletFields, SamlTools.serialize(wallet), refusals));
        item(out, "the shop trusts the key in the bank's metadata, and no other");
    }

    private static void act3Wrapping(StringBuilder out, Running shop, Refusals refusals) throws IOException, InterruptedException {
        line(out, "THREE. Signature wrapping: one assertion checked, another read.");
        Stopped first = signedInAtTheBank(shop, "ana");
        Document doc = first.response();
        SamlTools.stripOwnSignature(SamlTools.response(doc));
        Element evil = SamlTools.unsignedCopyOfAssertion(doc);
        SamlTools.setNameId(evil, "admin");
        Element signed = SamlTools.firstAssertion(doc);
        signed.getParentNode().insertBefore(evil, signed);
        section(out, "THE WRAPPED RESPONSE (the attacker's edit of a genuine one):");
        tree(out, SamlTools.response(doc), "");
        section(out, "THE RESULT (Spring Security with OpenSAML):");
        item(out, "-> " + post(shop, first, doc, refusals));
        item(out, "the signed assertion is not the one a reader would take first");

        section(out, "ONE THING CHANGED AT A TIME (the input, then the result):");
        Stopped second = signedInAtTheBank(shop, "ana");
        doc = second.response();
        SamlTools.stripOwnSignature(SamlTools.response(doc));
        evil = SamlTools.unsignedCopyOfAssertion(doc);
        SamlTools.setNameId(evil, "admin");
        signed = SamlTools.firstAssertion(doc);
        signed.getParentNode().appendChild(evil);
        item(out, "the bank's signed assertion first, an unsigned admin one second");
        item(out, "  -> " + post(shop, second, doc, refusals));
        Stopped third = signedInAtTheBank(shop, "ana");
        doc = third.response();
        SamlTools.stripSignatures(doc);
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        item(out, "no signature anywhere, NameID admin");
        item(out, "  -> " + post(shop, third, doc, refusals));
        Stopped fourth = signedInAtTheBank(shop, "ana");
        String bomb = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">&x;</samlp:Response>";
        item(out, "<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>");
        item(out, "  -> " + post(shop, fourth.browser(), fourth.fields(), bomb, refusals));
        section(out, "THE RESULT:");
        item(out, "the shop reads only what a trusted key signed; keep it patched");
    }

    private static void act4Statement(StringBuilder out, KeycloakLab keycloak, Running shop, Refusals refusals) throws Exception {
        line(out, "FOUR. The statement itself is checked.");
        Stopped s = signedInAtTheBank(shop, "ana");
        String xml = SamlTools.decode(s.fields().get("SAMLResponse"));
        section(out, "A GENUINE RESPONSE, POSTED ONCE:");
        item(out, "-> " + post(shop, s.browser(), s.fields(), xml, refusals));
        section(out, "ONE THING CHANGED AT A TIME (the input, then the result):");
        Shopper stranger = new Shopper();
        item(out, "the same response, replayed from another browser");
        item(out, "  -> " + post(shop, stranger, s.fields(), xml, refusals));
        item(out, "the same response, replayed from the same browser");
        item(out, "  -> " + post(shop, s.browser(), s.fields(), xml, refusals) + "  (request used up)");
        Stopped late = signedInAtTheBank(shop, "ana");
        Thread.sleep(4_500);
        item(out, "posted 4 seconds after issue; lifetime 2 s, skew 1 s");
        item(out, "  -> " + post(shop, late.browser(), late.fields(), SamlTools.decode(late.fields().get("SAMLResponse")), refusals));
        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replaceFirst("(<saml2:Issuer[^>]*>)[^<]+", "$1http://other-shop.example/saml/metadata")));
        Shopper.Page other = new Shopper().navigatePost(keycloak.issuer("bank") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH);
        Map<String, String> otherFields = Shopper.hiddenFields(other.body());
        Document otherDoc = SamlTools.parse(SamlTools.decode(otherFields.get("SAMLResponse")));
        item(out, "signed by the bank, Audience " + text(SamlTools.firstAssertion(otherDoc), "Audience").replace("http://", ""));
        item(out, "  -> " + post(shop, b, otherFields, SamlTools.decode(otherFields.get("SAMLResponse")), refusals));
        section(out, "THE RESULT:");
        item(out, "a signature says who wrote it; these checks say whether it is");
        item(out, "for this shop, this request, and now");
    }

    private static void act5Bill(StringBuilder out, KeycloakLab keycloak, int port) throws Exception {
        line(out, "FIVE. The bill: keys, metadata and XML.");
        try (Running shop = Running.start(keycloak, port, "bank")) {
            Refusals refusals = shop.context().getBean(Refusals.class);
            Stopped now = signedInAtTheBank(shop, "ana");
            section(out, "BEFORE THE KEY CHANGE (metadata read once, at start):");
            item(out, "a sign-in -> " + post(shop, now.browser(), now.fields(), SamlTools.decode(now.fields().get("SAMLResponse")), refusals));
            int added = keycloak.addSigningKey("bank");
            Stopped s = signedInAtTheBank(shop, "ana");
            section(out, "THE BANK ADDS A NEW SIGNING KEY (HTTP " + added + ") AND USES IT:");
            item(out, "the next sign-in -> " + post(shop, s.browser(), s.fields(), SamlTools.decode(s.fields().get("SAMLResponse")), refusals));
            item(out, "<- EVERY SHOPPER IS REFUSED");
        }
        try (Running shop = Running.start(keycloak, port, "bank")) {
            Refusals refusals = shop.context().getBean(Refusals.class);
            Stopped s = signedInAtTheBank(shop, "ana");
            section(out, "AFTER THE SHOP RELOADS THE BANK'S METADATA:");
            item(out, "the next sign-in -> " + post(shop, s.browser(), s.fields(), SamlTools.decode(s.fields().get("SAMLResponse")), refusals));
        }
        section(out, "THE BILL:");
        item(out, "a key rotation needs the shop to reload metadata first,");
        item(out, "  and a plan for the hours between");
        item(out, "XML signatures are hard: the shop uses OpenSAML through");
        item(out, "  Spring Security, and its patches matter");
    }

    private static void section(StringBuilder out, String header) {
        out.append("  ").append(header).append('\n');
    }

    private static void item(StringBuilder out, String text) {
        out.append("    ").append(text).append('\n');
    }

    private static String issuerOf(Element e) {
        return e.getElementsByTagNameNS(SamlTools.SAML, "Issuer").item(0).getTextContent().trim();
    }

    private static String text(Element e, String name) {
        return e.getElementsByTagNameNS(SamlTools.SAML, name).item(0).getTextContent().trim();
    }

    /** An address of the shop, with its port (which changes on every run) replaced by a placeholder. */
    private static String shopPath(Running shop, String url) {
        return url.startsWith(shop.url()) ? "<shop>" + url.substring(shop.url().length()) : url;
    }

    /** The last part of an algorithm URI: "...xmldsig-more#rsa-sha256" -> "rsa-sha256". */
    private static String algorithm(Element signedInfo, String name) {
        String uri = ((Element) signedInfo.getElementsByTagNameNS(SamlTools.DSIG, name).item(0)).getAttribute("Algorithm");
        uri = uri.endsWith("#") ? uri.substring(0, uri.length() - 1) : uri;
        return uri.substring(Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/')) + 1);
    }

    private static String jsonField(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "?";
    }

    private static String verdicts(java.util.List<SignatureByHand.Check> checks) {
        return checks.stream().map(c -> Boolean.toString(c.rsaVerifies())).collect(java.util.stream.Collectors.joining(", "));
    }

    /** The document's outline: the response and its assertions (name, signed or not), indented by depth. */
    private static void tree(StringBuilder out, Element response, String indent) {
        boolean signedResponse = false;
        for (org.w3c.dom.Node n = response.getFirstChild(); n != null; n = n.getNextSibling()) {
            signedResponse |= n instanceof Element e && "Signature".equals(e.getLocalName());
        }
        item(out, "samlp:Response" + (signedResponse ? ", signed" : ", its own signature removed"));
        org.w3c.dom.NodeList assertions = response.getElementsByTagNameNS(SamlTools.SAML, "Assertion");
        for (int i = 0; i < assertions.getLength(); i++) {
            Element a = (Element) assertions.item(i);
            boolean signed = a.getElementsByTagNameNS(SamlTools.DSIG, "Signature").getLength() > 0;
            item(out, "  saml:Assertion NameID " + text(a, "NameID") + (signed ? ", signed by the bank" : ", unsigned"));
        }
    }
}
