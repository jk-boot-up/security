# OpenID Connect with Spring Security and Keycloak, Explained

## The idea in one sentence

OpenID Connect tells the store who signed in as an issuer and a subject, in a token the library checks, and everything after that, the account, the session and the logout, is the store's own work.

## The 5 acts

### 1. OAuth gives access; OpenID Connect adds who signed in

The store sends Ana to Keycloak with the scope openid email and a state, a nonce and a PKCE challenge kept for her browser. She signs in at Keycloak, never at the store, and the store redeems the code itself. The ID token's claims include acr, at_hash, aud, auth_time, azp, email, email_verified, exp, iat, iss, nonce, sid, sub and typ. Ana is signed in to the store as the bank realm and her subject, with her e-mail address, verified. The nonce in her ID token is the one the store sent. The reply holds two tokens: an ID token for the store, typ ID, audience store-web, and an access token for the bank's API, typ Bearer, with no audience claim. The access token handed to the store as proof of sign-in is refused by Spring's decoder: its audience is missing. A sign-in without the openid scope gets no ID token. A callback from another sign-in, carried to Ana's browser, is refused.

### 2. Checking the ID token

Against a local provider that signs claims Keycloak would never issue: the bank's own ID token is accepted. The same claims signed with a key the bank never published, under the bank's key id: refused, invalid signature. Issuer https://bank.example/ with one extra slash, and issuer https://wallet.example signed by the bank: refused. Audience budget-app: refused. Audiences store-web and budget-app with no azp: refused; with azp store-web: accepted. 29 seconds past its expiry, inside the 30 second skew: accepted; 31 seconds past: refused as expired. No subject: refused. A header saying alg none: refused. The nonce is checked by the sign-in itself, against the one kept in the browser's session.

### 3. Discovery and keys

The store is configured with one value, the issuer. It reads the discovery document, which names the issuer it asked for, and the authorization, token and key addresses all start with the issuer's own address. A document at that address naming the issuer https://wallet.example is refused: one provider cannot pose as another. A document whose keys are on https://keys.example is accepted by Spring Security, which checks the issuer and not where the keys are, so the issuer address must be https and trusted. Keycloak publishes 1 signing key. The bank adds a second key and signs new tokens with it; it now publishes 2, and the store, which has not seen the new key id, reloads the key set on its own: tokens signed with the old key and with the new key are both accepted.

### 4. Who signed in: issuer and subject, never the email

The bank says Ana's subject has the e-mail address ana@mail.example, verified. The wallet says its user also has the e-mail address ana@mail.example, not verified. A store that keys accounts by e-mail address gives Mallory, who signed in with the wallet, the account of Ana: account takeover. Keyed by issuer and subject, Ana gets the account of Ana and Mallory gets a new account for wallet and her own subject. Userinfo with Ana's access token answers 200 with the same subject as the ID token. Userinfo with the ID token instead is refused with 401.

### 5. Signing out, and the bill

Ana signs out at the store: the store's session ended. Her browser is sent to Keycloak's logout endpoint with id_token_hint and post_logout_redirect_uri, Keycloak ends its session and sends the browser back to the store, and signing in again asks for her password. Ana is signed in on a laptop and a phone, two Keycloak sessions. Keycloak ends the laptop's session through the admin API and sends the store a back-channel logout token: the laptop is signed out and the phone stays signed in. A made-up logout token is refused with 400, and an ID token sent as a logout token is refused too, with a 500 from this version of Spring Security, and the phone stays signed in. Back-channel logout makes the store post to its own logout address, which by default is built from the request and so names host.docker.internal; the store is told its own address.

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
