# SAML Single Sign-On and Signature Wrapping, Explained

## The idea in one sentence

A service provider must trust only the identity provider's pinned key, read only the element that the signature covers, and check the statement's issuer, audience, time, request, destination and freshness, using a maintained library.

## The 5 acts

### 1. Signing in through the bank

The request: an AuthnRequest with ID req-1 from https://shop.example, asking for the answer at https://shop.example/acs. The response: an assertion
with ID a1, issued at 2026-03-01T10:00:00Z by https://idp.bank.example, whose NameID is ana@bank.example. It answers req-1 (InResponseTo), is sent
to https://shop.example/acs (Recipient), is meant for https://shop.example (Audience), and is valid from NotBefore 09:59:00 (issued minus 60
seconds) to NotOnOrAfter 10:05:00 (issued plus 300 seconds). Its signature sits inside the assertion and covers #a1. Every provider accepts a
genuine response: the weaknesses only show when somebody edits the document.

### 2. How the bank signs the assertion

The input is the assertion a1 as XML. Step 1 (the enveloped transform): the Signature element itself is left out. Step 2: exclusive XML
canonicalization writes the assertion in one exact form, 614 bytes, so that spaces and namespace prefixes cannot change the result. Step 3: the
DigestValue is the SHA-256 of those bytes, in base64; it is the same on every run because the ids and the clock are fixed. Step 4: SignedInfo
holds the Reference URI #a1 and that DigestValue, 640 bytes once canonicalized. Step 5: the SignatureValue is RSA-SHA256 over SignedInfo with the
bank's private key, 256 bytes; the key is new on every run, so the value is shown as a placeholder. The shop checks it in reverse, by hand: the
SHA-256 of the same bytes equals the DigestValue, and RSA verification of SignedInfo with the bank's public key succeeds; the library's
XMLSignature.validate agrees. The signature covers #a1 and nothing else.

### 3. A valid signature is not the same as the bank's signature

The input: the same XML shape with NameID admin@bank.example, signed with the attacker's own key, whose public half sits in KeyInfo. Before the
fix, the trusting provider validates with the key the message carries: the signature is valid, and the attacker is signed in as
admin@bank.example. After the fix, the pinned-key provider validates with the bank's key, known in advance: the signature is not valid, and the
response is refused. Then one name is edited in a genuine response: the DigestValue in SignedInfo still says what the bank signed, but the SHA-256
of the edited assertion is different, so the signature is not valid.

### 4. Signature wrapping: one assertion checked, another read

The input: the attacker keeps the genuine signed assertion a1 untouched but moves it into samlp:Extensions, and puts an unsigned copy with ID
forged-1 and NameID admin@bank.example first in the response. Before the fix, the pinned-key provider asks "is the first signature valid with the
bank's key?" (yes, it covers #a1) and then reads the first assertion in the document: forged-1, admin@bank.example. After the fix, the strict
provider requires exactly one assertion, directly under the response, with its own signature inside it, whose validated reference is that same
element. The wrapped response has two assertions and is refused. The strict provider refuses the self-signed and the edited responses as well.

### 5. The statement itself is checked

A genuine assertion with a valid signature can still be unfit. At now 2026-03-01T10:00:00Z the strict provider checks: the issuer is
https://idp.bank.example; NotBefore minus 60 seconds is not after now and now is before NotOnOrAfter plus 60 seconds; the audience is
https://shop.example; InResponseTo is req-1, the request this browser made; and assertion a2 has not been seen before. Ana is signed in. Then one
thing at a time: the same response again (the provider remembers assertion ids), an assertion whose NotOnOrAfter was 09:10:00, one meant for
https://other.example, one answering req-999, and a document with a DOCTYPE, refused before it is parsed any further.

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
