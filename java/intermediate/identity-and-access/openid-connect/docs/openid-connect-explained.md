# OpenID Connect, Explained

## The idea in one sentence

OpenID Connect adds a signed ID token to OAuth 2.0 that says who signed in, and a relying party must check it against the provider it discovered from
one configured issuer: exact issuer, its own audience, the time and its own nonce, then identify the customer by issuer and subject.

## The 5 acts

### 1. OAuth gives access; OpenID Connect adds who signed in

The store sends Ana to the bank with the scope "openid email", and keeps a state and a nonce for her browser. The bank's reply holds two tokens:
an ID token for the store, and an access token for the bank's API. The ID token's claims are iss, sub, aud, exp, iat, auth_time, nonce, acr, amr,
sid, email and email_verified. The ID token is for store-web; the access token is for https://api.bank.example. Ana is signed in to the store as
https://bank.example | ana-0001, at level urn:example:loa:2 with a password and a one-time code. The access token handed to the store as proof of
sign-in is refused: its type is at+jwt, not an ID token. A sign-in that asks without the openid scope is refused too: the reply gives API access,
not a sign-in.

### 2. Checking the ID token, as OpenID Connect Core requires

The bank's own ID token is accepted. The same claims signed with a key the bank never published, under the bank's key id, are refused: the signature
does not match. The issuer https://bank.example/, with one extra slash, is refused, because the issuer must equal the configured issuer exactly, and
so is https://wallet.example signed by the bank. The audience budget-app is refused. Audiences store-web and budget-app with no azp are refused;
with azp store-web they are accepted. Twenty-nine seconds past its expiry the token is accepted, inside the 30-second skew; thirty seconds past, it is
refused. A nonce from another sign-in is refused, so a token from another login cannot be replayed into this one, and so is a token with no nonce at
all. A header saying alg none is refused before anything else is read.

### 3. Discovery and keys

The store is configured with one value, the issuer https://bank.example. It reads https://bank.example/.well-known/openid-configuration, which names
the issuer https://bank.example, and loads the key bank-2026-a from https://bank.example/jwks.json. A document at that address naming the issuer
https://wallet.example is refused, so one provider cannot pose as another, and so is a document whose keys are on https://keys.example. The bank adds
a second key and signs new tokens with it; after a reload the store holds bank-2026-a and bank-2026-b, and tokens signed with either are accepted.
The bank retires bank-2026-a; after a reload, the old key's token is refused as an unknown key id and a new token is accepted. Over real HTTP on this
machine, metadata and keys load and an ID token from that provider is accepted.

### 4. Who signed in: issuer and subject, never the email

The bank says ana-0001 has the email ana@mail.example, verified. wallet.example says its user mal-3301 also has the email ana@mail.example, not
verified. A store that keys accounts by email lets Mallory sign in with the wallet and get Ana's account: an account takeover. Keyed by issuer and
subject there are 2 accounts, https://bank.example | ana-0001 and https://wallet.example | mal-3301, and Mallory's account has no email, because
the wallet did not verify it. Userinfo with Ana's access token returns sub ana-0001 and her email, with the same subject as the ID token. Userinfo
with the ID token instead is refused, and so is the access token after it expires.

### 5. Signing out, and the bill

Ana signs out at the store: the store's session ends, and her browser is sent to https://bank.example/logout with id_token_hint,
post_logout_redirect_uri and state. Ana is then signed in on a laptop and a phone: 2 store sessions. The bank ends the laptop's session and sends a
back-channel logout token, typed logout+jwt, with the logout event, a sid and no nonce: accepted. One store session is left; the laptop is signed out
and the phone still signed in. A logout token with a nonce is refused, so is one without the logout event, and so is an ID token sent as a logout
token. The bill: OpenID Connect has many checks and options, so production code uses a certified library and a real provider; the ID token is for
the store only; and signing out must reach every party.

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
