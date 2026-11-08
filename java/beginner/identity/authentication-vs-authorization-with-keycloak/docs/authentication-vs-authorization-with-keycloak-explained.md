# Authentication vs Authorization with Keycloak, Explained

## The idea in one sentence

With a login server the shop holds no passwords: it checks a signed token's signature, expiry, issuer and audience, and then applies its own rules to the roles inside it, remembering that those roles are a copy that can be out of date until the token expires.

## The 6 acts

### 1. No login in the shop, only tokens

The shop holds no passwords. Five requests go to GET /me, each with a different token. With no token, with garbage, and with a token signed with another key, the response is 401. A token the login server signed,
good for 300 seconds, gets 200 and the answer ana [ROLE_CUSTOMER]. 301 seconds later the same token gets 401: the expiry is inside the signed token, so the shop needed no call to the login server to know.

### 2. Inside a token, and its signature

The token as sent is 642 characters: three base64url parts joined by dots. Decoded, the header is {"alg":"RS256"}, the claims say who (sub ana), for which API (aud shop-api), from which login server (iss),
the roles (realm_access.roles = customer), and when it was issued and expires (iat, exp), and the signature is 256 bytes. The shop checks the signature in four steps. One: the signed text is the first two parts, header.claims, exactly as sent.
Two: its SHA-256 hash, 32 bytes, starting a1ec3b27. Three: the shop raises the signature to the power e (65537) modulo n, the login server's public key, and gets 256 bytes: 00 01, 202 bytes of ff, 00, nineteen bytes that say SHA-256, and a 32-byte hash.
Four: that hash equals the hash of step two, so the login server signed exactly this text. When customer is changed to admin in the claims, the text's hash becomes 4b680136..., the hash in the signature is still a1ec3b27..., they differ, and the shop answers 401.
`SignatureSteps` does these steps by hand, and a test proves it always agrees with the library.

### 3. Who is it, and what may they do

The token says realm_access.roles; the shop turns each role into an authority: "ROLE_" plus the role in capitals, so customer becomes ROLE_CUSTOMER. Then six requests. Ana reads her own order A-1: 200. Ana reads Ben's order B-2: 403, logged in but not allowed.
Support Sam reads B-2: 200. The sales report answers 403 to Ana and to Sam and 200 to root, the admin. 401 means the shop does not know who is asking; 403 means it knows, and the answer is no.

### 4. A real signature is not enough

Both tokens carry a good signature. The first has iss http://login.elsewhere.example/realms/other, the second has aud another-api. The shop wants iss http://login.shop.example/realms/shop and aud shop-api,
so both get 401. Without the audience check, a token the user got for any other API of the same login server would open this one. The shop checks the signature, the expiry, the issuer and the audience before its own rules run.

### 5. The roles in a token are a copy

Before the demotion, root's token says admin and the sales report answers 200. Root is then demoted to customer in the login server's directory. After the demotion, the same token still gets 200, because the shop only sees the copy of the roles inside it.
301 seconds later the token has expired and gets 401, and a new token says customer: the report answers 403 and root's own pages 200. A short lifetime is the control: the stale window is at most the lifetime of the token, here 300 seconds.

### 6. Ending a token early, and the bill

Before revoking, root's admin token (jti token-10) opens the report twice. Its id is put on the revocation list, and after revoking the next request gets 401 at once. The bill: the list was read 3 times for 3 requests, on every request,
which is the call that tokens were meant to avoid. Without a list the shop makes no call, but the token works until it expires. Refresh tokens give long sessions with short access tokens, and the login server can refuse to refresh.
And the login server is now one more system that must be kept running, patched and backed up.

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
