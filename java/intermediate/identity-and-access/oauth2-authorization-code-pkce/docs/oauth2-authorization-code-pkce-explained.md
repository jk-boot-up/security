# OAuth 2.0 Authorization Code Flow with PKCE, Explained

## The idea in one sentence

In the authorization code flow with PKCE, the customer signs in at the bank, the browser carries only a short-lived single-use code to an exactly registered
address, and the app swaps it directly for tokens by proving it holds the verifier it made for that sign-in, so neither the app nor anyone on the way ever
sees the password, and a leaked code is worthless.

## The 5 acts

### 1. The authorization code flow, step by step

Step 1, the request: the budgeting app sends Ana's browser to https://bank.example/authorize with response_type=code, client_id=budget-app,
redirect_uri=https://budget.example/callback, scope=accounts.read, a random state, code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM and
code_challenge_method=S256. Step 2: Ana signs in at the bank, never at the app, and agrees to accounts.read. Step 3, the response: 302 to the callback with
code and state; the code is 43 random characters and works for 60 seconds, once. Step 4, the request: the app posts to the token endpoint directly, with
grant_type=authorization_code, the code, the redirect address, its client id and code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk. Step 5, the
response: an access token with expires_in=300, a refresh token and scope accounts.read. Step 6: GET /accounts with the access token answers 200, ana: 2
accounts. The app never saw Ana's password, and the browser never saw the access token. The same flow over a real local HTTP server answers 302, 200 and 200.

### 2. PKCE: a code is useless without the verifier

The verifier is the RFC 7636 example, dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk: 43 characters from A-Z, a-z, 0-9 and - . _ ~. The S256 challenge is made
in steps: the 43 characters as 43 ASCII bytes (64 42 6a 66 ...); SHA-256 of those bytes, always 32 bytes (13 d3 1e 96 ...); then base64url, where every 3
bytes become 4 characters of 6 bits: 13 d3 1e is 000100 111101 001100 011110, the numbers 4 61 12 30, the characters E9Me. All 32 bytes become 43
characters with no = padding: E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM, the value RFC 7636 publishes and the value Java's Base64 gives.
Only the challenge travels through the browser. When the code leaks, another app redeeming it with no verifier, or with a verifier it made up, is refused
with invalid_grant, PKCE verification failed; the budgeting app with the real verifier gets tokens. code_challenge_method=plain, which would send the verifier
itself, and a request with no code_challenge are refused with invalid_request.

### 3. Exact redirect addresses, state, and codes that work once

The budgeting app registered exactly https://budget.example/callback, and the bank compares the requested address with it character for character. That
address gets a code. A path that climbs out with ../steal, a host that only starts with the same name (budget.example.evil.test), an added ?next=evil and a
change of letter case are all refused, with no redirect. A check that the address starts with https://budget.example would have sent the code to three of
them. In a login forgery, Ana's browser opens a callback link with Mallory's code and state; the app checks the state against Ana's browser session and
refuses it with invalid_state. A code redeemed once gives tokens and GET /accounts answers 200; the same code redeemed again is refused with invalid_grant,
and the first access token now answers 401, because a reused code means it leaked. A code redeemed 61 seconds after it was issued is refused as expired,
even with the real verifier.

### 4. Scopes and refresh tokens

The app asked for accounts.read, Ana agreed to that, and the token carries only that scope: GET /accounts answers 200 and POST /payments answers 403
insufficient_scope. Asking for payments.write too, which the app never registered, is refused with invalid_scope. When access token A1 runs out, the app
sends refresh token R1 and gets a new access token A2 and a new refresh token R2; R2 differs from R1, and R1 is now spent. A thief who copied R1 replays
it: refused, and because a spent refresh token was used twice, the bank revokes the whole family. R2, held by the real app, is refused too, and A2 answers 401.
The theft is contained, at the price of Ana signing in again. An access token 300 seconds old answers 401.

### 5. Current practice, and the bill

The implicit flow, response_type=token, which put the access token in the address bar, is refused with unsupported_response_type: only the authorization
code flow is offered. The password grant, which would give the app Ana's password, is refused with unsupported_grant_type. Both are removed by RFC 9700,
the OAuth 2.0 Security Best Current Practice, and by OAuth 2.1. The statements service is a confidential client that runs on a server, so it also proves
who it is: with a wrong client secret it is refused with invalid_client, and with the right secret but a code it was never given it is refused with
invalid_grant, unknown code. A phone app cannot keep a secret, so PKCE protects its code. This small server leaves out discovery, signed tokens, consent
records and key rotation: in production, run a real authorization server, as in the -with-keycloak version.

## The verdict

Use the authorization code flow with PKCE for every client, public or confidential. Register exact redirect URIs. Bind state to the browser session, or use
PKCE and OpenID Connect's nonce together. Keep codes single-use and short-lived. Ask for the narrowest scopes. Rotate refresh tokens or bind them to the
client. Never use the implicit flow or the password grant. And use a maintained authorization server and client library.

## How to recognise this in code you did not write

- `response_type=code` and `code_challenge_method=S256` in an authorization address.
- `?code=...&state=...` in the address after signing in.
- `invalid_grant`, `invalid_scope` and `unsupported_grant_type` in token endpoint errors.
- "Connect your account" consent pages that list what an app may do.

## Where you have already met this

- "Connect your bank account" buttons in budgeting and accounting apps, and the bank page that asks what the app may see.
- "Sign in with" buttons, which use OpenID Connect, built on this flow (the openid-connect project).
- Addresses ending in `?code=...&state=...` after signing in somewhere.
- Advisories about stolen authorization codes, open redirects and leaked refresh tokens.
