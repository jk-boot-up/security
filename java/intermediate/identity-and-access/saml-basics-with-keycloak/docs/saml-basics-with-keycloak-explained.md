# SAML Basics with Keycloak, Explained

## The idea in one sentence

A SAML service provider believes a statement only if the identity provider's own key signed it, the shop asked for it, it is for this shop, and it is still current.

## The 5 acts

### 1. Signing in through the bank

The request: the shop at http://127.0.0.1:<port> sends Ana's browser to the bank with an unsigned AuthnRequest whose Issuer is the shop's entity id
<shop>/saml2/service-provider-metadata/keycloak, asking for the answer at <shop>/login/saml2/sso/keycloak. The response: Ana signs in at the bank,
never at the shop. The bank's realm (<keycloak>/realms/bank) answers that request (InResponseTo), sends it to the shop's address (Destination),
names ana, is for the shop's entity id only (Audience) and lasts 2 seconds (NotOnOrAfter minus NotBefore). It carries 2 signatures: the Response as a
whole and the Assertion on its own. Redone by hand: the shop's key is the RSA 2048-bit key in the bank's metadata; the signatures use rsa-sha256,
sha256 and exclusive canonicalization; for each, the SHA-256 of the covered element equals its DigestValue and RSA verification succeeds. The browser
posts the response, and the shop's page says name ana, email ana@bank.example.

### 2. A valid signature is not the same as the bank's signature

One name edited in the bank's response, ana to admin, with both signatures left in place: the SHA-256 of the Response and of the Assertion no
longer equal their DigestValues, and the shop refuses (invalid_signature). The attacker's response: admin, both signatures removed and made again
with the attacker's own key, whose public half is in KeyInfo; RSA verifies with the attacker's key (true, true) and fails with the bank's (false,
false); refused. A genuine response from the wallet realm, which answers the shop's own request, is signed by the wallet's key; refused. The shop
trusts the key in the bank's metadata and no other.

### 3. Signature wrapping: one assertion checked, another read

The input: a genuine response with its own signature removed, an unsigned copy of the assertion naming admin put first, and the bank's signed
assertion for ana second. A reader that takes the first assertion would sign in admin; OpenSAML refuses it (invalid_signature), because every
assertion it reads must be signed by the trusted key. Then one thing at a time: the bank's signed assertion first and an unsigned admin one second
(invalid_signature), no signature anywhere with NameID admin (invalid_signature), and a response with a DOCTYPE that reads a file
(malformed_response_data). The shop reads only what a trusted key signed, which is why the library is kept patched.

### 4. The statement itself is checked

A genuine response posted once signs Ana in. Then one thing at a time, each response genuinely signed by the bank: the same response replayed from
another browser, and from the same browser, is refused (invalid_in_response_to), because the shop's request is answered once; a response posted 4
seconds after it was issued, when the assertion lasts 2 seconds and the shop allows 1 second of skew, is refused (invalid_assertion); and a response
the bank signed for another shop, whose Audience is other-shop.example/saml/metadata, is refused (invalid_assertion). A signature says who wrote
the statement; these checks say whether it is for this shop, this request, and now.

### 5. The bill: keys, metadata and XML

Before the key change, the shop has read the bank's metadata once, at start, and a sign-in works. The bank adds a new RSA signing key with a higher
priority (HTTP 201) and signs with it from then on: the next sign-in is refused (invalid_signature), and so is every shopper's. After the shop
restarts and reads the metadata again, the next sign-in works. So a key rotation needs the shop to reload the metadata first, and a plan for the
hours between; and because XML signatures are hard, the shop uses OpenSAML through Spring Security, whose patches matter.

## The verdict

Use a maintained library, trust only the key in the identity provider's metadata, read only the signed assertion, keep each request single-use, check the audience and the dates, refuse a DOCTYPE, and plan key rotation with the shop's metadata reload.

## How to recognise this in code you did not write

- `saml2Login()` and `assertingparty.metadata-uri` in a Spring Boot application.
- A `SAMLResponse` field in a browser's network tab.
- `invalid_signature` or `invalid_in_response_to` in a log.
- A metadata file with an `X509Certificate`.

## Where you have already met this

- An XML blob with `<saml2p:Response>` in a browser's network tab.
- `saml2Login()` in a Spring Security configuration.
- An identity provider's metadata file with an `X509Certificate`.
- An error such as "Invalid InResponseTo" or "Invalid signature" after a key change.
