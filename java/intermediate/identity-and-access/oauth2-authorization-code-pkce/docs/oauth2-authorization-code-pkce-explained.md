# OAuth 2.0 Authorization Code Flow with PKCE, Explained

## The idea in one sentence

In the authorization code flow with PKCE, the customer signs in at the bank, the browser carries only a short-lived single-use code to an exactly registered
address, and the app swaps it directly for tokens by proving it holds the verifier it made for that sign-in, so neither the app nor anyone on the way ever
sees the password, and a leaked code is worthless.

## The 5 acts

### 1. The authorization code flow, step by step

The budgeting app sends Ana's browser to the bank's authorization endpoint with the response type code, its client id, its registered redirect address, the
scope accounts.read, a state and a PKCE code challenge. Ana signs in at the bank, never at the app, and agrees. The bank sends the browser back with a code of
43 random characters that works for 60 seconds, once. The app checks the state, then calls the bank's token endpoint directly with the code and its code
verifier, and receives an access token for 300 seconds and a refresh token. It reads Ana's accounts with the access token: 200. The app never saw Ana's
password, and the browser never saw the access token. The same flow over a real local HTTP server answers 302, 200 and 200.

### 2. PKCE: a code is useless without the verifier

The RFC 7636 example verifier gives the S256 challenge E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM, the published value. Only the challenge travels
through the browser: the request carries it, and not the verifier. When the code leaks, say through a log or a browser history, another app that redeems
it with no verifier, or with one it made up, is refused because PKCE verification fails. The budgeting app redeems it with the real verifier. A request with
the plain method, which would send the verifier itself, is refused, and so is a request with no challenge at all.

### 3. Exact redirect addresses, state, and codes that work once

The budgeting app registered exactly https://budget.example/callback. That address gets a code; a path that climbs out with `..`, a host that only starts
with the same name, an added query and a change of letter case are all refused at the bank, with no redirect. A prefix or wildcard match would have sent
Ana's code to three of them. Mallory sends Ana a link to the app's callback carrying Mallory's own code, to sign Ana into Mallory's account; the app checks
the state against Ana's browser session and refuses it. A code redeemed once works; the same code redeemed again is refused, and the access token from the
first redemption is revoked, because a reused code means it leaked. A code redeemed 61 seconds after it was issued is refused as expired.

### 4. Scopes and refresh tokens

The app asked for accounts.read, Ana agreed to that, and the token carries only that scope. GET /accounts answers 200; POST /payments with the same token
answers 403 insufficient_scope. Asking for payments.write too, which the app never registered, is refused as an invalid scope. When the access token runs
out, the app refreshes it and gets a new access token and a new refresh token. The old refresh token, copied by a thief and used again, is refused, and the
whole family is revoked: the newest refresh token held by the real app is refused too, and the newest access token answers 401. One reuse contains the
theft. An access token 300 seconds old answers 401.

### 5. Current practice, and the bill

The implicit flow, which put the access token in the address bar, is refused as an unsupported response type. The password grant, which gives the app Ana's
password, is refused as an unsupported grant type. Both are removed by OAuth 2.0 Security Best Current Practice, RFC 9700, and by OAuth 2.1. The
statements service is a confidential client, so it also authenticates: with a wrong secret it is refused, and with its secret it still needs a real code and
verifier. A public app on a phone cannot keep a secret, which is why PKCE, not a secret, protects its code. This server leaves out discovery, signed tokens,
consent records, key rotation and account recovery: in production, run a real authorization server, as in the -with-keycloak version.

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
