# SAML Basics with Keycloak, Explained

## The idea in one sentence

A SAML service provider believes a statement only if the identity provider's own key signed it, the shop asked for it, it is for this shop, and it is still current.

## The 5 acts

### 1. Signing in through the bank

The shop sends the shopper to the bank with an authentication request from the shop's entity id, naming its assertion consumer address, unsigned, with an id of its own. Ana signs in at the bank, never at the shop. The bank answers with a response signed as a whole and an assertion signed on its own: 2 signatures. The response answers the shop's request, is addressed to the shop's address and comes from the bank's realm. The assertion names ana, is for the shop's entity id only, and lasts seconds. The shopper's browser posts it to the shop and she is signed in as ana, with her e-mail address.

### 2. A valid signature is not the same as the bank's signature

The bank's response with the name edited to admin is refused: invalid_signature. Admin, signed correctly with the attacker's own key, two valid XML signatures, is refused: invalid_signature. A genuine response from the wallet, signed by the wallet and answering the shop's own request, is refused: invalid_signature. The shop trusts the key in the bank's metadata, and no other key, however valid the signature.

### 3. A valid signature on one assertion, and another one read

With the response unsigned, an admin assertion first and the bank's signed one second, the response is refused: invalid_signature. With the bank's signed assertion first and an admin one second, it is refused too. With no signature anywhere, admin is refused. A response with a DOCTYPE that reads a file is refused as malformed. The shop reads the assertion that a trusted key signed, and only that one; a library does this and is kept patched.

### 4. The statement itself is checked

A genuine response, first time: signed in as ana. The same response replayed from another browser is refused: invalid_in_response_to, because the shop's request was used up. Replayed from the same browser, it is refused too. A response posted 4 seconds after it was issued, to an assertion that lasts 2, is refused: invalid_assertion. A genuine response the bank signed for another shop is refused: invalid_assertion, because the signature is the bank's and the audience is not this shop. A signature says who wrote the statement; the other checks say whether it is for this shop, this request, now.

### 5. The bill: keys, metadata and XML

The shop reads the bank's metadata once, when it starts; a sign-in works. The bank adds a new signing key and signs with it from now on; the next sign-in is refused: invalid_signature, and so is every shopper's. The shop restarts and reads the metadata again; the next sign-in works. So a bank that rotates its key needs the shop to reload its metadata first, and a plan for the hours between. XML signatures are hard to get right, which is why the shop uses OpenSAML through Spring Security, and why its patches matter.

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
