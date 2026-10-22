# OAuth 2.0 Authorization Code with PKCE and Keycloak, Explained

## The idea in one sentence

The authorization code flow lets an app act for a user without their password, and it is safe only because the client's settings, the redirect check, PKCE, the state and one-time codes are all enforced.

## The 5 acts

### 1. The flow, step by step

Ana's browser goes to Keycloak's authorization endpoint with response type code, the client id, the exact registered redirect address, the scope, a state and an S256 code challenge. Ana signs in at Keycloak, never at the app, and a consent page asks whether budget-app may read her accounts. Keycloak redirects the browser to the registered address with the state, a session state, the issuer and the code. The app checks the state, then calls the token endpoint directly with the code and the verifier. Keycloak answers with an access token for 300 seconds, a refresh token and an ID token. The accounts API accepts the access token: 200, ana, 2 accounts. The token is for accounts-api, asked for by budget-app, carries Ana's id and no password or e-mail, and without a token the API answers 401.

### 2. PKCE: a code is useless without the verifier

The RFC 7636 example verifier gives the published S256 challenge. Only the challenge travels through the browser. A code leaks, and another app redeems it with no verifier: refused, PKCE code verifier not specified. Then the budgeting app, with the real verifier, is refused too: Code not valid, because a failed try burns the code. The leak is useless, and Ana signs in again. A new code with a verifier the thief made up is refused: PKCE verification failed. A new code with the real verifier gives tokens. A request with the plain method, or with no challenge at all, is refused with invalid_request.

### 3. Exact redirects, state, and codes that work once

The app registered exactly https://budget.example/callback. That address gets a code. The path trick, the lookalike host, the query, the upper-case host and the trailing slash are refused by Keycloak with an error page and no redirect. A prefix or wildcard match would have sent Ana's code to the others. Mallory's link carries her own code; the app checks the state against Ana's browser session and refuses it. A code is redeemed once and the access token works. The same code redeemed again is refused, and Keycloak's own check of the first access token now gives 401, a reused code ends the session. The API, which checks the signature and dates itself, still gives 200: a local check cannot know. A code redeemed after 6 seconds, in a realm that gives codes 5, is refused.

### 4. Scopes and refresh tokens

The token carries openid accounts.read. GET /accounts is 200, and POST /payments with the same token is 403 insufficient_scope. Asking for payments.write, which the app never registered, is refused with invalid_scope. The app refreshes the token and gets a new access token and a new refresh token. The old refresh token, copied by a thief and used again, is refused: the maximum allowed reuse was exceeded. The newest refresh token, held by the real app, is refused too, the session no longer has the client, and the newest access token fails Keycloak's own check with 401. One reuse ends the session, so the theft is contained. An access token 5 minutes and 31 seconds old is refused by the API.

### 5. Current practice, and the bill

The implicit flow, which put the access token in the address bar, is refused: the client is not allowed to initiate browser login with that response type. The password grant, which gives the app Ana's password, is refused: the client is not allowed direct access grants. Both are removed by OAuth 2.0 Security Best Current Practice, RFC 9700, and by OAuth 2.1. The statements service is a confidential client, so it also authenticates; with a wrong secret it is refused. With its secret and a code it does not have, it is refused. With its secret and a real code but no verifier, it is refused too: a secret does not replace PKCE. A public app on a phone cannot keep a secret, which is why PKCE protects its code.

## The verdict

Use the authorization code flow with S256 PKCE for every client, register exact redirect addresses, switch off the implicit flow and the password grant, rotate refresh tokens, keep access tokens short, check the state in the app, and have the API check the issuer, audience, dates and scope of every token.

## How to recognise this in code you did not write

- `code_challenge` and `code_verifier` in a mobile app.
- "Invalid parameter: redirect_uri" from the authorization server.
- `oauth2ResourceServer` in a Spring Security configuration.
- A realm file or a client with `implicitFlowEnabled` or `directAccessGrantsEnabled` set to true.

## Where you have already met this

- A "Sign in with" button, and a consent screen asking to read your accounts.
- `code_challenge` and `code_verifier` in a mobile app's network log.
- "Invalid parameter: redirect_uri" from Keycloak when a callback address was mistyped.
- `spring.security.oauth2.client` and `oauth2ResourceServer` in a Spring Boot project.
