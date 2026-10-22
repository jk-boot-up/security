package com.jk.explore.security.samlkeycloak;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Runs the shop's single sign-on story in five numbered acts against a real Keycloak in Docker and a real Spring Security SAML 2.0 service provider. */
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
        line(out, "== 1. Signing in through the bank ==");
        Shopper browser = new Shopper();
        Shopper.Page toBank = browser.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = Shopper.hiddenFields(toBank.body());
        Document authn = SamlTools.parse(SamlTools.decode(request.get("SAMLRequest")));
        Element authnRequest = authn.getDocumentElement();
        line(out, "the shop sends the shopper to the bank with an authentication request: from " + authnRequest.getElementsByTagNameNS(SamlTools.SAML, "Issuer").item(0).getTextContent().replaceAll(":\\d+", ":<port>")
                + ", to answer at " + authnRequest.getAttribute("AssertionConsumerServiceURL").replaceAll(":\\d+", ":<port>") + ", unsigned, with an id of its own");
        Stopped s = signedInAtTheBank(shop, "ana");
        Document response = s.response();
        Element root = SamlTools.response(response);
        Element assertion = SamlTools.firstAssertion(response);
        line(out, "ana signs in at the bank, never at the shop; the bank answers with a response signed as a whole and an assertion signed on its own: " + SamlTools.describeSignatures(response));
        line(out, "the response answers the shop's request: " + s.requestId().startsWith("ARQ") + ", is addressed to the shop's address: " + root.getAttribute("Destination").endsWith(Running.ACS_PATH)
                + ", comes from the bank's realm: " + root.getElementsByTagNameNS(SamlTools.SAML, "Issuer").item(0).getTextContent().equals(keycloak.issuer("bank")));
        line(out, "the assertion names ana: " + SamlTools.nameId(response) + ", is for the shop's entity id only: "
                + assertion.getElementsByTagNameNS(SamlTools.SAML, "Audience").item(0).getTextContent().endsWith(Running.ENTITY_PATH) + ", and lasts seconds: "
                + (assertion.getElementsByTagNameNS(SamlTools.SAML, "Conditions").getLength() == 1));
        Refusals refusals = shop.context().getBean(Refusals.class);
        line(out, "the shopper's browser posts it to the shop: " + post(shop, s, response, refusals));
        line(out, "the shop's page for her: " + s.browser().get(shop.url() + "/me").body().replaceAll("\"sessionIndexes\":\\d+", "\"sessionIndexes\":n"));
    }

    private static void act2WrongSigner(StringBuilder out, KeycloakLab keycloak, Running shop, Refusals refusals) throws IOException, InterruptedException {
        line(out, "== 2. A valid signature is not the same as the bank's signature ==");
        Stopped edited = signedInAtTheBank(shop, "ana");
        Document doc = edited.response();
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        line(out, "the bank's response with the name edited to admin   -> " + post(shop, edited, doc, refusals));
        Stopped selfSigned = signedInAtTheBank(shop, "ana");
        doc = selfSigned.response();
        SamlTools.stripSignatures(doc);
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        java.security.KeyPair attacker = SamlTools.attackerKey();
        SamlTools.sign(SamlTools.firstAssertion(doc), attacker);
        SamlTools.sign(SamlTools.response(doc), attacker);
        line(out, "admin, signed correctly with the attacker's own key -> " + post(shop, selfSigned, doc, refusals) + "  (" + SamlTools.describeSignatures(doc) + ", every one of them valid XML)");
        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replace(keycloak.issuer("bank") + "/protocol/saml", keycloak.issuer("wallet") + "/protocol/saml")));
        Shopper.Page walletPage = new Shopper().navigatePost(keycloak.issuer("wallet") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH);
        Map<String, String> walletFields = Shopper.hiddenFields(walletPage.body());
        Document wallet = SamlTools.parse(SamlTools.decode(walletFields.get("SAMLResponse")));
        line(out, "a genuine response from the wallet, signed by the wallet -> " + post(shop, b, walletFields, SamlTools.serialize(wallet), refusals) + "  (answers the shop's request: "
                + SamlTools.response(wallet).getAttribute("InResponseTo").equals(request.get("RelayState") == null ? "" : SamlTools.parse(SamlTools.decode(request.get("SAMLRequest"))).getDocumentElement().getAttribute("ID")) + ")");
        line(out, "the shop trusts the key in the bank's metadata, and no other key, however valid the signature");
    }

    private static void act3Wrapping(StringBuilder out, Running shop, Refusals refusals) throws IOException, InterruptedException {
        line(out, "== 3. A valid signature on one assertion, and another one read ==");
        Stopped first = signedInAtTheBank(shop, "ana");
        Document doc = first.response();
        SamlTools.stripOwnSignature(SamlTools.response(doc));
        Element evil = SamlTools.unsignedCopyOfAssertion(doc);
        SamlTools.setNameId(evil, "admin");
        Element signed = SamlTools.firstAssertion(doc);
        signed.getParentNode().insertBefore(evil, signed);
        line(out, "response unsigned, an admin assertion first, the bank's signed one second -> " + post(shop, first, doc, refusals));
        Stopped second = signedInAtTheBank(shop, "ana");
        doc = second.response();
        SamlTools.stripOwnSignature(SamlTools.response(doc));
        evil = SamlTools.unsignedCopyOfAssertion(doc);
        SamlTools.setNameId(evil, "admin");
        signed = SamlTools.firstAssertion(doc);
        signed.getParentNode().appendChild(evil);
        line(out, "response unsigned, the bank's signed assertion first, an admin one second -> " + post(shop, second, doc, refusals));
        Stopped third = signedInAtTheBank(shop, "ana");
        doc = third.response();
        SamlTools.stripSignatures(doc);
        SamlTools.setNameId(SamlTools.firstAssertion(doc), "admin");
        line(out, "no signature anywhere, admin -> " + post(shop, third, doc, refusals));
        Stopped fourth = signedInAtTheBank(shop, "ana");
        String bomb = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">&x;</samlp:Response>";
        line(out, "a response with a DOCTYPE that reads a file -> " + post(shop, fourth.browser(), fourth.fields(), bomb, refusals));
        line(out, "the shop reads the assertion that a trusted key signed, and only that one; a library does this and is kept patched");
    }

    private static void act4Statement(StringBuilder out, KeycloakLab keycloak, Running shop, Refusals refusals) throws Exception {
        line(out, "== 4. The statement itself is checked ==");
        Stopped s = signedInAtTheBank(shop, "ana");
        String xml = SamlTools.decode(s.fields().get("SAMLResponse"));
        line(out, "genuine response, first time -> " + post(shop, s.browser(), s.fields(), xml, refusals));
        Shopper stranger = new Shopper();
        line(out, "the same response replayed from another browser -> " + post(shop, stranger, s.fields(), xml, refusals));
        line(out, "the same response replayed from the same browser -> " + post(shop, s.browser(), s.fields(), xml, refusals) + "  (the shop's request was used up)");
        Stopped late = signedInAtTheBank(shop, "ana");
        Thread.sleep(4_500);
        line(out, "a response posted 4 seconds after it was issued, to an assertion that lasts 2 -> " + post(shop, late.browser(), late.fields(), SamlTools.decode(late.fields().get("SAMLResponse")), refusals));
        Shopper b = new Shopper();
        Shopper.Page toBank = b.navigate(shop.url() + "/saml2/authenticate/keycloak", null, null, keycloak.issuer("bank"));
        Map<String, String> request = new LinkedHashMap<>(Shopper.hiddenFields(toBank.body()));
        request.put("SAMLRequest", SamlTools.encode(SamlTools.decode(request.get("SAMLRequest")).replaceFirst("(<saml2:Issuer[^>]*>)[^<]+", "$1http://other-shop.example/saml/metadata")));
        Shopper.Page other = new Shopper().navigatePost(keycloak.issuer("bank") + "/protocol/saml", Shopper.formBody(request), "ana", "demo-password", shop.url() + Running.ACS_PATH);
        Map<String, String> otherFields = Shopper.hiddenFields(other.body());
        line(out, "a genuine response the bank signed for another shop -> " + post(shop, b, otherFields, SamlTools.decode(otherFields.get("SAMLResponse")), refusals) + "  (the signature is the bank's, the audience is not this shop)");
        line(out, "a signature says who wrote the statement; the checks above say whether it is for this shop, this request, now");
    }

    private static void act5Bill(StringBuilder out, KeycloakLab keycloak, int port) throws Exception {
        line(out, "== 5. The bill: keys, metadata and XML ==");
        try (Running shop = Running.start(keycloak, port, "bank")) {
            Refusals refusals = shop.context().getBean(Refusals.class);
            Stopped now = signedInAtTheBank(shop, "ana");
            line(out, "the shop reads the bank's metadata once, when it starts; a sign-in now -> " + post(shop, now.browser(), now.fields(), SamlTools.decode(now.fields().get("SAMLResponse")), refusals));
            int added = keycloak.addSigningKey("bank");
            Stopped s = signedInAtTheBank(shop, "ana");
            line(out, "the bank adds a new signing key (" + added + ") and signs with it from now on; the next sign-in -> " + post(shop, s.browser(), s.fields(), SamlTools.decode(s.fields().get("SAMLResponse")), refusals) + "  <- EVERY SHOPPER IS REFUSED");
        }
        try (Running shop = Running.start(keycloak, port, "bank")) {
            Refusals refusals = shop.context().getBean(Refusals.class);
            Stopped s = signedInAtTheBank(shop, "ana");
            line(out, "the shop restarts and reads the metadata again; the next sign-in -> " + post(shop, s.browser(), s.fields(), SamlTools.decode(s.fields().get("SAMLResponse")), refusals));
        }
        line(out, "so a bank that rotates its key needs the shop to reload its metadata first, and a plan for the hours between");
        line(out, "XML signatures are hard to get right, which is why the shop uses OpenSAML through Spring Security, and why its patches matter");
    }
}
