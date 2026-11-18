# Tokens in the Browser with Spring Boot: a BFF and DPoP, Explained

## The idea in one sentence

A page that holds tokens gives them to every script on it, so keep the tokens on a server, use cookies with the right attributes, and bind any token that must reach the browser to a key.

## The 5 acts

### 1. Tokens kept in the browser

The app signs Ana in at Keycloak as the public client bank-spa and writes both tokens, each a JWT signed by Keycloak, to localStorage. The access token's middle part, decoded, says sub
a0a0a0a0-0000-4000-8000-000000000001, azp bank-spa and scope accounts.read openid payments.write; its exp is iat plus 300 seconds. The refresh token's exp is iat plus 1800 seconds, the
session's idle time. The app reads the balance: 200, 125000 cents. A third-party script reads localStorage and sends both tokens to its own server. From elsewhere the access token reads
the balance: 200. 331 seconds later the API refuses it: 401 invalid_token. But the refresh token gets a new access token from Keycloak, which reads the balance again.

### 2. A backend for the frontend keeps the tokens on the server

The BFF, a Spring Boot app, signs Ana in with oauth2Login as the confidential client bank-bff. The browser gets the session cookie __Host-bff with Path=/, Secure, HttpOnly and SameSite=Strict,
and an XSRF-TOKEN cookie that the page's own script may read and copy into a header. The BFF's OAuth2AuthorizedClientService holds 2 tokens; the browser holds 0. The app reads the balance through
the BFF: 200. The same script reads document.cookie, sees only XSRF-TOKEN, and takes 0 tokens. While the page is open it can still ask the BFF for the balance: 200; what it took gets 401 from
elsewhere. Six minutes later the access token has expired; the browser sends only the cookie, and the BFF refreshes the token on the server: 200, with a new access token.

### 3. Requests started by another site

A link on evil.example asks for the balance and a hidden form posts a transfer, both while Ana is signed in. In a modern browser SameSite=Strict keeps the cookie off both: the link gets 401,
the form 403, with no session and no CSRF token. In an older browser that ignores SameSite the cookie goes, but the X-XSRF-TOKEN header does not: 403. A script on evil.example that adds the
header itself makes the browser ask first in a CORS preflight; the answer has no Access-Control-Allow-Origin, so the request is never sent. The bank's own page sends the cookie and the
XSRF-TOKEN value in the header: 201. Transfers made: 1.

### 4. Tokens bound to a key, with DPoP

The app's key is a P-256 key made from a fixed seed, so the thumbprint is the same on every run. Its public half written as crv, kty, x and y, with no spaces, is 126 characters; SHA-256 of that,
in base64url, is lq6Aws5FE-qf5LOaH8K26iRrqGPFaWQ0v8IwPTXG6UU, the same as Nimbus's computeThumbprint. Keycloak, asked by the client bank-spa-dpop with a proof, returns token type DPoP and puts
that value in cnf.jkt. Each request carries a proof: a JWT of type dpop+jwt, signed with ES256, with a random jti, htm, htu, iat and ath, the hash of the access token. With a fresh proof:
200. The token alone: 401 invalid_token. A proof from the thief's key, the app's request sent again, the proof for GET /accounts used for a transfer, a proof made 120 seconds ago and a proof
with no ath: 401 invalid_dpop_proof. At Keycloak the refresh token is refused with the thief's key and with no key, and gives new tokens with the app's key.

### 5. The bill

Two BFF servers, each with its own session store: Ana signs in on the first, her cookie reaches the second, which answers 401, so the sessions need a shared store. Every API call passes
through one more server, which must be run, scaled and kept up to date. Clearing the browser's storage loses the DPoP key: the token is bound to lq6Aws5FE-qf..., the new key's thumbprint is
Vr-SIKG4_2B7..., so the API answers 401 invalid_dpop_proof and Ana signs in again. And a script on the open page can still ask the key for a proof and read the balance: 200. DPoP stops theft,
not use while the page is open; keep scripts off the page with a strict Content-Security-Policy, and keep tokens out of it with the BFF.

## The verdict

Use a backend for the frontend for a bank's web app, give the browser a __Host- cookie and an XSRF header, refresh on the server, and where a token must reach the browser, use DPoP and keep scripts off the page with a strict Content-Security-Policy.

## How to recognise this in code you did not write

- `localStorage.setItem` with a token in front-end code.
- A `__Host-` cookie, `HttpOnly`, `SameSite=Strict`.
- `Authorization: DPoP` and a `DPoP` header.
- An `XSRF-TOKEN` cookie and an `X-XSRF-TOKEN` header.

## Where you have already met this

- `localStorage.setItem("access_token", ...)` in a tutorial.
- A cookie named `__Host-` something, `HttpOnly` and `SameSite=Strict`.
- `TokenRelay` or an API gateway that adds the access token to calls.
- `Authorization: DPoP ...` and a `DPoP:` header in a request log.
