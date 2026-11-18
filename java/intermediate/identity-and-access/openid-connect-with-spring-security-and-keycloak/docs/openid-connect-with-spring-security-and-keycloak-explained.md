# OpenID Connect with Spring Security and Keycloak, Explained

## The idea in one sentence

OpenID Connect tells the store who signed in as an issuer and a subject, in a token the library checks, and everything after that, the account, the session and the logout, is the store's own work.

## The 5 acts

### 1. OAuth gives access; OpenID Connect adds who signed in

The request: Ana's browser goes to <keycloak>/realms/bank/protocol/openid-connect/auth with response_type code, client_id store-web, scope
openid email, a random state and nonce kept in her session, and code_challenge (the SHA-256 of a random verifier) with method S256. She types her
password at Keycloak, never at the store, and the store redeems the code server to server. The ID token has 19 claims: iss <keycloak>/realms/bank,
sub a0a0a0a0-0000-4000-8000-000000000001, aud and azp store-web, typ ID, the nonce sent in this sign-in, and the verified email ana@mail.example.
The result: Ana is signed in as <keycloak>/realms/bank | a0a0a0a0-0000-4000-8000-000000000001. The reply holds an ID token (typ ID, aud store-web)
and an access token (typ Bearer, no aud claim). The access token offered as a sign-in is refused (invalid claims {aud=null}); a sign-in with only
the email scope gets no ID token; and a callback from another sign-in, opened in Ana's browser, is refused because no sign-in in her browser has that
state.

### 2. Checking the ID token

The store's settings: issuer https://bank.example, client_id store-web, the provider's key lab-key, 30 seconds of clock skew. Before signing: the
header {"kid":"lab-key","alg":"RS256"} and the claims aud, exp, iat, iss, sub, with exp = iat + 300 seconds. The header part is base64url(header),
42 characters; the claims part 132 characters; the signing input is the two joined by a dot; the signature is RSA over the SHA-256 of that input,
which with a 2048-bit key is 256 bytes, 342 base64url characters. Spring Security checks the signature with the published key named by kid, then exp
and iat with 30 seconds of skew, then iss exactly, sub, aud and azp: accepted. One thing changed at a time: a key never published (Invalid
signature), iss with an extra slash, another iss, aud budget-app, two audiences without azp, no sub, and alg none are refused; two audiences with azp
store-web are accepted. 29 seconds past exp is accepted, 31 seconds past is refused as expired. The nonce is checked by the sign-in itself, with the
state, as act one showed.

### 3. Discovery and keys

The store is configured with one value, issuer-uri <keycloak>/realms/bank. It reads <keycloak>/realms/bank/.well-known/openid-configuration, which
names the issuer, the authorization, token, jwks_uri and end-session addresses, all under the issuer. Spring Security checks that the issuer equals the
configured one, and the addresses start with the issuer's own address. A test provider serves forged metadata: one naming https://wallet.example is
refused; one whose jwks_uri is https://keys.example/jwks.json is accepted, because Spring Security checks the issuer, not where the keys are, so the
issuer address must be https and trusted. Key rotation on the real Keycloak: it publishes 1 signing key; the bank adds a second and signs new tokens
with it, so 2 keys are published and the new token's kid is new; the store reloads the key set on its own, and tokens signed with either key are
accepted.

### 4. Who signed in: issuer and subject, never the email

The bank says its user a0a0a0a0-0000-4000-8000-000000000001 has the address ana@mail.example, verified. The wallet says its user
c0c0c0c0-0000-4000-8000-000000003301 has the same address, not verified. Before the fix the store keys accounts by e-mail address, and Mallory, signing
in with the wallet, gets the account of Ana: an account takeover. After the fix accounts are keyed by issuer and subject: Ana gets the account of Ana,
and Mallory gets a new account for wallet | c0c0c0c0-0000-4000-8000-000000003301. Userinfo with Ana's access token answers 200 with her subject, the
same as in the ID token; the ID token instead is refused with 401.

### 5. Signing out, and the bill

Ana signs out at the store with a CSRF-protected POST: the store's session ends, and her browser is sent to
<keycloak>/realms/bank/protocol/openid-connect/logout with id_token_hint and post_logout_redirect_uri. Keycloak ends its session and sends her back;
signing in again asks for her password. Then Ana is signed in on a laptop and a phone, with two Keycloak sessions and two sids. Keycloak ends the
laptop's session (its admin API answers 204) and posts a logout token with that sid to the store's /logout/connect/back-channel/bank: the laptop is
signed out and the phone is not. A made-up logout token is refused with 400 and an ID token sent as a logout token with 500; the phone stays signed
in. The bill: use a certified library, keep the ID token in the store, and make sign-out reach every party.

## The verdict

Use a certified library and the discovery address of a provider you trust over https, key customers by issuer and subject, treat e-mail as an attribute, keep the ID token away from APIs, and make sign-out reach the store, the provider and every other application.

## How to recognise this in code you did not write

- `http.oauth2Login()` and `issuer-uri` in a Spring Boot application.
- An `email` column used as a unique key for accounts.
- A `sid` claim in an ID token.
- A logout that only clears the store's own cookie.

## Where you have already met this

- `spring.security.oauth2.client.registration` and `provider.*.issuer-uri` in an application.yml.
- A "Sign in with" button.
- `http.oauth2Login()` and `http.oidcLogout()` in a security configuration.
- An account table with an `email` column used as the key.
