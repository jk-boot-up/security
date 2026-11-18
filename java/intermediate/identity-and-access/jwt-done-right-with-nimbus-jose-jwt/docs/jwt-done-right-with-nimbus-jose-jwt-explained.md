# JWT Done Right with Nimbus JOSE + JWT, Explained

## The idea in one sentence

A standard library verifies the signature and most claims, and the API still decides the algorithm, the type, where keys come from, the scope and the lifetime.

## The 6 acts

### 1. The bank signs a token with Nimbus

The bank's authorization server builds a JWSHeader, {"kid":"bank-2026-01","typ":"at+jwt","alg":"ES256"}, 51 bytes as JSON, and a JWTClaimsSet of 143
bytes: sub ana, aud accounts-api, nbf 1800000000, scope accounts:read, iss https://auth.bank.example, exp 1800000300 and iat 1800000000. Base64url turns
them into 68 and 191 characters. ES256 is ECDSA on the curve P-256 over SHA-256: the signing input, header part, a dot and claims part, is 260 ASCII
bytes; its SHA-256 starts abc368652950a7f5691a490899cb2e91; the signature is R and S, 32 bytes each, 64 bytes or 86 characters. The token is
68 + 1 + 191 + 1 + 86 = 347 characters. ECDSA takes a fresh random number for every signature, so the same claims signed twice give the same first
two parts and a different signature, and both verify with the bank's public key. That is why the signature part is printed as a placeholder.

### 2. Parsing is not verifying

Only the claims part is replaced: "sub":"ana" becomes "sub":"mallory", so part 2 grows from 191 to 196 characters, while the header and Ana's
86-character signature stay. SignedJWT.parse alone reads the claims and checks nothing: getSubject() returns mallory. The validator, a
DefaultJWTProcessor, verifies part 3 over parts 1 and 2 with the public key of bank-2026-01 first: false. It refuses the token with Signed JWT rejected:
Invalid signature. Anyone can read the claims, since base64url is an encoding, not encryption, so no secret belongs in them.

### 3. The server chooses the algorithm, never the token

The API's settings are in AccessTokenValidator: a JWSVerificationKeySelector for ES256 only and a DefaultJOSEObjectTypeVerifier for at+jwt only. As a
check that Nimbus signs standard ES256, the JDK's own SHA256withECDSAinP1363Format verifies Ana's token: true. Four headers are sent with Ana's claims.
ES256 with at+jwt, Ana's token, is accepted. none with no signature is refused (Unsecured (plain) JWTs are rejected). HS256, with the bank's public key
used as the shared secret, is refused (Another algorithm expected, or no matching key(s) found). An ID token, typ id+jwt, is refused (JOSE header typ
(type) id+jwt not allowed).

### 4. Every claim that matters is checked

The API expects the issuer https://auth.bank.example and the audience accounts-api, with 30 seconds of clock skew, checked by DefaultJWTClaimsVerifier;
the scope accounts:read and the lifetime are two checks of ours. Ana's token has iat and nbf 1800000000 and exp 1800000300, so Nimbus accepts it while now
<= exp + 30 = 1800000330. At the moment of issue and at exp + 20 it is accepted; at exp + 31 it is refused (Expired JWT). One claim changed per token,
each signed by the bank: nbf 1800000120 is refused (JWT before use time), iss https://auth.partner.example (JWT iss claim value rejected), aud loyalty-api
(JWT aud claim rejected), and scope cards:read by our own check (missing scope accounts:read).

### 5. Keys by id, from the one URL in the settings

The bank publishes its key set at http://127.0.0.1:<port>/jwks.json, and the API's JWKSourceBuilder uses only that URL, caches it for 2000 ms and
refetches at most once per 1800 ms. Ana's token and 100 more cost 1 fetch. A token with kid attacker-key is refused (no matching key) and costs a second
fetch; 100 more cost 0 more. A token naming bank-2026-01 but signed with the attacker's key is refused (Invalid signature). A jku header pointing at the
attacker's own key set and a jwk header carrying the attacker's key are refused; the attacker's server was asked 0 times. In the rotation, bank-2026-07
is published first, and after the cache time the old token is still accepted (3 fetches) and a token signed with the new key is accepted. Once
bank-2026-01 is retired, the old token is still accepted at once, from the cache; after the cache time it is refused (5 fetches). Every fetch went to the
configured URL.

### 6. The bill: a token cannot be taken back

Ana holds an access token, which stops working after 300 + 30 = 330 seconds, and a refresh token, 43 random characters the bank keeps and looks up. At
120 seconds the bank removes her access and revokes the refresh token. At 240 the access token is still accepted, because the API checks the token and
not the bank's records, while the refresh token is refused as revoked. At 331 the access token is refused (Expired JWT). Our own policy allows at most
900 seconds, 15 minutes, so a token with exp = iat + 86400 is refused: lifetime longer than the policy allows. The library parses, verifies and checks
the claims; the policy is ours.

## The verdict

Fix the algorithm and the type, give the library one key set URL and never anything from the token, verify before you believe, check issuer and audience exactly, keep access tokens short, check your own policy, publish a new key before using it, and know how long a retired key lives in your cache.

## How to recognise this in code you did not write

- `JWSVerificationKeySelector` and `DefaultJWTProcessor` in the code.
- `/.well-known/jwks.json` in a configuration file.
- A `kid` header in a decoded token.
- A token that still works after the user was disabled.

## Where you have already met this

- `NimbusJwtDecoder.withJwkSetUri(...)` in Spring Security's resource server.
- `/.well-known/jwks.json` on an identity provider.
- A token with `"alg":"none"` in a pen test report.
- `kid` in a JWT header.
