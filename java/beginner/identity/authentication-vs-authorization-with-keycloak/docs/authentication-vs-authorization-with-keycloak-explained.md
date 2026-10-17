# Authentication vs Authorization with Keycloak, Explained

## The idea in one sentence

With a login server the shop holds no passwords: it checks a signed token's signature, expiry, issuer and audience, and then applies its own rules to the roles inside it, remembering that those roles are a copy that can be out of date until the token expires.

## The 5 acts

### 1. No login in the shop, only tokens

The shop holds no passwords. With no token the answer is 401, and so it is for garbage and for a token signed with another key. A token the login server signed, good for 300 seconds, is accepted. 301 seconds later the same token is refused,
and the shop needed no call to the login server to know: the expiry is inside the signed token.

### 2. Who is it, and what may they do

The roles in the token's realm_access.roles claim become authorities named ROLE_CUSTOMER, ROLE_SUPPORT and ROLE_ADMIN. Ana reads her own order A-1 with 200 and Ben's order B-2 with 403: logged in, not allowed.
Support Sam reads Ben's order. The sales report answers 403 to customer and support and 200 to admin. 401 means the shop does not know who is asking, and 403 means it knows and the answer is no.

### 3. A real signature is not enough

A token can have a perfectly good signature and still not be meant for the shop. A correctly signed token from another issuer is refused. A correctly signed token meant for another API is refused too: without an audience check,
any API's token would open this one. The shop checks the signature, the expiry, the issuer and the audience before its own rules run.

### 4. The roles in a token are a copy

Root, an admin, opens the sales report. Root is then demoted to customer in the login server's directory. The token root already holds still opens the report, because the shop only sees the copy of the roles inside it.
After the token expires it is refused, and a new token carries the new roles: the report answers 403. A short lifetime is the control: the stale window is at most the lifetime of the token.

### 5. Ending a token early, and the bill

A revocation list can end a token at once: the admin token works, its id is put on the list, and the next request is refused. The price is that the list is consulted for every request, which is the call that tokens were meant to avoid.
Without a list the shop makes no call at all and the token can be used until it expires. Refresh tokens give long sessions with short access tokens, and the login server can refuse to refresh. And the login server is now one more system that must be kept running, patched and backed up.

## The verdict

Use a login server when several applications share logins. Keep access tokens short, check issuer and audience, map roles to authorities in one place, test the realm configuration like code, and treat the login server as critical infrastructure.
Use PKCE for public clients, avoid the password grant, and register exact redirect addresses.

## How to recognise this in code you did not write

- `spring.security.oauth2.resourceserver.jwt.issuer-uri` in an application's configuration.
- `Authorization: Bearer` headers and `/.well-known/openid-configuration` URLs.
- Keycloak, Auth0, Okta, Azure AD and Amazon Cognito: login servers.
- A realm or tenant export checked into a repository.

## Where you have already met this

- "Sign in with Google" and company single sign-on pages.
- `Authorization: Bearer ...` headers in API documentation.
- `.well-known/openid-configuration` and `jwks.json` URLs.
- Tokens pasted into jwt.io, with `exp` and `iss` claims.
