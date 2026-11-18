# OpenID Connect, Explained

## The idea in one sentence

OpenID Connect adds a signed ID token to OAuth 2.0 that says who signed in, and a relying party must check it against the provider it discovered from
one configured issuer: exact issuer, its own audience, the time and its own nonce, then identify the customer by issuer and subject.

## The 5 acts

### 1. OAuth gives access; OpenID Connect adds who signed in

The request: the store sends Ana's browser to https://bank.example/authorize with client_id store-web, redirect_uri https://shop.example/callback,
scope "openid email", and a random state and nonce kept for her browser. The response from the bank's token endpoint holds two tokens: an ID token
(typ JWT, aud store-web) and an access token (typ at+jwt, aud https://api.bank.example). The ID token's claims: iss https://bank.example, sub
ana-0001, aud store-web, iat 1800000000, exp 1800000300 (iat + 300 seconds), auth_time, the nonce that was kept, acr urn:example:loa:2 with amr
[pwd, otp], sid sid-1, and the verified email. The result: Ana is signed in as https://bank.example | ana-0001. The access token offered as proof of
sign-in is refused (not an ID token, typ at+jwt), and a sign-in that asks only for accounts.read gets no ID token at all.

### 2. Checking the ID token, as OpenID Connect Core requires

Before signing: the header {"alg":"EdDSA","typ":"JWT","kid":"bank-2026-a"} and the claims as JSON. The bank base64url-encodes each, joins them with
a dot into the signing input, and signs that with the Ed25519 private key bank-2026-a: 64 bytes, 86 characters in base64url. The token is the three
parts joined by dots. The store checks in this order: alg is EdDSA (its own choice), the key id is published, the Ed25519 signature verifies, iss
equals https://bank.example exactly, aud contains store-web, now is before exp plus 30 seconds of skew, and the nonce is the one this browser was
given. Then one thing at a time: a foreign key, an issuer with an extra slash, another issuer, another audience, two audiences without azp, a nonce
from another sign-in, no nonce and a header saying alg none are refused; two audiences with azp store-web are accepted. Twenty-nine seconds past exp
the token is accepted (exp + 30 is still after now); thirty seconds past it is refused as expired.

### 3. Discovery and keys

The configuration is one value, the issuer https://bank.example. The request is GET https://bank.example/.well-known/openid-configuration. The
response names the issuer, the authorization, token, userinfo and end-session endpoints, jwks_uri https://bank.example/jwks.json and the signing
algorithm EdDSA. The store checks that the issuer equals the configured one and that jwks_uri is on the issuer's own site. The key set holds
bank-2026-a, an OKP Ed25519 signing key whose x value the bank made from its private 32-byte seed. Metadata at the same address naming
https://wallet.example is refused, so one provider cannot pose as another, and so is metadata whose keys are on https://keys.example. Key rotation:
the bank adds bank-2026-b; after a reload both keys are there and both tokens are accepted; the bank retires bank-2026-a; after the next reload the
old token is refused (unknown key id) and the new one is accepted. Over real HTTP on this machine the metadata and the key loop-1 load and a token is
accepted.

### 4. Who signed in: issuer and subject, never the email

The bank says its user ana-0001 has the email ana@mail.example, verified. wallet.example says its user mal-3301 has the same email, not verified.
Before the fix the store keys accounts by email: Ana's account is ana@mail.example, and when Mallory signs in with the wallet she gets the same account:
one account, an account takeover. After the fix accounts are keyed by issuer and subject: https://bank.example | ana-0001 with her email, and
https://wallet.example | mal-3301 with no email, because an unverified email is not kept: two accounts. Userinfo with Ana's access token returns sub
ana-0001 and her email, the same subject as the ID token; the ID token instead is refused (not an access token), and so is the access token 301 seconds
later (expired).

### 5. Signing out, and the bill

Ana signs out at the store (RP-initiated logout): the store's session ends, and her browser is sent to https://bank.example/logout with
id_token_hint, post_logout_redirect_uri https%3A%2F%2Fshop.example%2Fsigned-out (the address, URL-encoded) and a random state. Then Ana is signed in on
a laptop (sid-3) and a phone (sid-5): two store sessions. The bank ends the laptop's session and sends a logout token: header typ logout+jwt, alg
EdDSA, kid bank-2026-a; claims iss, aud store-web, sub ana-0001, sid sid-3, jti lt-7, the back-channel logout event, and no nonce. It is accepted;
one store session is left, the laptop is signed out and the phone is not. A logout token with a nonce, one without the event, and an ID token sent as
a logout token are refused. The bill: use a certified library, keep the ID token in the store, and make sign-out reach every party.

## The verdict

Use OpenID Connect for sign-in, never a bare OAuth access token. Configure each provider by its issuer, load metadata and keys through discovery, and
let a certified library validate ID tokens. Bind state and nonce to the browser, identify customers by issuer and subject, never link accounts on an
email claim, and implement sign-out at both ends.

## How to recognise this in code you did not write

- `scope=openid` in authorization requests, and an `id_token` in token responses.
- `/.well-known/openid-configuration` documents and `jwks.json` key sets.
- Library settings for the issuer, client id and allowed clock skew.
- `end_session_endpoint` and `backchannel_logout_uri` in provider configuration.

## Where you have already met this

- "Sign in with ..." buttons on websites and apps.
- Addresses ending in `/.well-known/openid-configuration`, and key sets at `jwks.json`.
- `id_token`, `nonce` and `prompt=login` in redirect URLs.
- Advisories about account takeover through unverified email claims in social sign-in.
