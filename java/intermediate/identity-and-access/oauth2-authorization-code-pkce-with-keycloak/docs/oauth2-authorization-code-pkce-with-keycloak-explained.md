# OAuth 2.0 Authorization Code with PKCE and Keycloak, Explained

## The idea in one sentence

The authorization code flow lets an app act for a user without their password, and it is safe only because the client's settings, the redirect check, PKCE, the state and one-time codes are all enforced.

## The 5 acts

### 1. The flow, step by step

Step 1, the request: Ana's browser goes to <keycloak>/realms/bank/protocol/openid-connect/auth with response_type=code, client_id=budget-app,
redirect_uri=https://budget.example/callback, scope=openid accounts.read, a random state, code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM and
code_challenge_method=S256. Step 2: Ana signs in at Keycloak, never at the app, and the consent page asks for "Read your accounts". Step 3, the response:
302 to the registered address with state, session_state, iss (the bank realm) and code; the state comes back unchanged. Step 4, the request: the app posts
grant_type=authorization_code, its client id, the code, the redirect address and code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk to the token
endpoint. Step 5, the response: an access token that lives 300 seconds, a refresh token, an ID token and the scope openid accounts.read. Inside the access
token, a JWT signed by Keycloak: aud=accounts-api, azp=budget-app, the bank realm as issuer, Ana's id as subject, and no password or e-mail. Step 6:
GET /accounts with the token answers 200, ana: 2 accounts; without a token, 401.

### 2. PKCE: a code is useless without the verifier

The verifier is the RFC 7636 example, dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk. The S256 challenge is made in steps: the 43 characters as 43 ASCII
bytes (64 42 6a 66 ...); SHA-256 of those bytes, 32 bytes (13 d3 1e 96 ...); base64url, where 13 d3 1e is 000100 111101 001100 011110, the numbers
4 61 12 30, the characters E9Me; all 32 bytes give E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM, the published value. Only the challenge travels through
the browser. A thief who redeems a leaked code with no verifier is refused with invalid_grant, PKCE code verifier not specified, and the failed try burns
the code: the budgeting app with the real verifier then gets Code not valid, and Ana signs in again. A new code with a made-up verifier of 43 x's is
refused with PKCE verification failed: Code mismatch; with the real verifier, tokens are issued. Keycloak refuses code_challenge_method=plain and a request
with no code_challenge with invalid_request.

### 3. Exact redirect addresses, state, and codes that work once

The budgeting app registered exactly https://budget.example/callback and Keycloak compares the requested address with it exactly. Only that address gets a
code; ../steal, budget.example.evil.test, ?next=evil, an upper-case host and a trailing slash each get the page Invalid parameter: redirect_uri, with no
redirect. A check that the address starts with https://budget.example would have sent the code away four times. In a login forgery, Ana's browser opens a
callback link with the attacker's code and state; the app compares the state with Ana's browser session and refuses it. A code redeemed once gives tokens
and GET /accounts answers 200; the same code again is refused with Code not valid, and Keycloak ends the session: its userinfo check of the first access
token answers 401, while the API, which checks the signature and the dates itself, still answers 200 until the token expires. In this realm a code lives
10 seconds; redeemed 11 seconds later, even with the real verifier, it is refused.

### 4. Scopes and refresh tokens

The app asked for openid accounts.read and the token carries exactly that. GET /accounts answers 200 and POST /payments answers 403 insufficient_scope.
Asking for payments.write too, which the client was never given, is refused with invalid_scope. When access token A1 runs out, the app sends refresh
token R1 and gets A2 and R2, both new. A thief who copied R1 replays it: refused with Maximum allowed refresh token reuse exceeded, and Keycloak ends the
session. R2, held by the real app, is refused with Session doesn't have required client, and Keycloak's userinfo check of A2 answers 401. An access
token 331 seconds old is refused by the API: it lives 300 seconds, and the API allows 30 seconds for clocks that differ.

### 5. Current practice, and the bill

The implicit flow, response_type=token, is refused: Client is not allowed to initiate browser login with given response_type, Implicit flow is disabled
for the client. The password grant is refused: Client not allowed for direct access grants. Both are removed by RFC 9700 and by OAuth 2.1, and here they
are switched off on the client. The statements service is a confidential client: a wrong client secret is refused with Invalid client or Invalid client
credentials; the right secret with a code it was never given gets Code not valid; and the right secret with a real code but no verifier gets PKCE code
verifier not specified, because a secret does not replace PKCE. The accounts API keeps no session and checks Keycloak's signing key, the issuer, the
audience accounts-api, the dates and the scope of every token. In production: TLS, backups, key rotation and monitoring of logins.

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
