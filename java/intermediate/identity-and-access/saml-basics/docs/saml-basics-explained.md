# SAML Single Sign-On and Signature Wrapping, Explained

## The idea in one sentence

A service provider must trust only the identity provider's pinned key, read only the element that the signature covers, and check the statement's issuer, audience, time, request, destination and freshness, using a maintained library.

## The 4 acts

### 1. Signing in through the bank

The IdP signs the assertion for ana@bank.example and the shop checks it. All providers accept a genuine response: the weaknesses only show when somebody edits the document.

### 2. A valid signature is not the same as the bank's signature

The signature carries the signer's public key. A provider that validates with that key proves only that someone signed the document, and the attacker signs "admin@bank.example" with their own key: the trusting provider signs them in as admin.
A provider that pins the identity provider's key refuses it. Editing the name inside a genuine signed assertion changes what the signature covers, and every provider refuses that.

### 3. A valid signature on one assertion, and another one read

The attacker leaves the genuine signed assertion intact but moves it into an Extensions element, and puts an unsigned copy naming admin@bank.example first. The pinned-key provider asks "is the first signature valid?" (yes, the genuine one, with the bank's key)
and then reads the first assertion in the file, which is the forged one: signed in as admin. The strict provider requires exactly one assertion, directly under the response, with its own signature inside it, whose validated reference is that same element. The wrapped response has two
assertions and is refused.

### 4. The statement itself is checked

A genuine assertion with a valid signature can still be unfit: a response used a second time (the provider remembers assertion ids), an assertion past its NotOnOrAfter, one meant for a different audience, one that answers a request this browser did not make
(InResponseTo), and one sent to another address. A response with a DOCTYPE is refused before it is parsed any further.

## The verdict

Prefer OpenID Connect. If you must accept SAML: use a maintained library, pin keys from signed metadata, require exactly one signed assertion that is the element you read, validate conditions and replay, refuse DOCTYPEs, and test with wrapping and
self-signing attacks.

## How to recognise this in code you did not write

- `ds:Signature` with `ds:KeyInfo` in SAML responses.
- `getElementsByTagName("Assertion").item(0)` after a signature check.
- `InResponseTo`, `Audience` and `NotOnOrAfter` in assertions.
- A finding titled "XML Signature Wrapping" or CWE-347.

## Where you have already met this

- `<saml:Assertion>`, `<ds:Signature>` and `<saml:AudienceRestriction>` in browser traffic of enterprise sign-in.
- "XML signature wrapping" in lists of famous SSO vulnerabilities.
- SAML metadata files and certificates exchanged during setup.
- A finding titled "Improper Verification of Cryptographic Signature" with CWE-347.
