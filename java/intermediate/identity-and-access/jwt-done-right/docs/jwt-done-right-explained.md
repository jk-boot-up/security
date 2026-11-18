# JWT Done Right, Explained

## The idea in one sentence

A JWT is believable only after the server has checked it against its own settings: its algorithm and type, a key it already holds, the signature, and
then the expiry, issuer, audience and scope; and since it cannot be taken back, it must expire soon.

## The 6 acts

### 1. How the bank builds a token

The header is JSON of 51 bytes: the algorithm EdDSA, the type at+jwt and the key id bank-2026-01. The claims are JSON of 143 bytes: the issuer
https://auth.bank.example, the subject ana, the audience accounts-api, the scope accounts:read, issued at 1800000000 (2027-01-15 08:00:00 UTC), not
before the same second, and expiring 300 seconds later. Base64url turns every 3 bytes into 4 characters: the first three header bytes {"a are 7b 22 61,
whose 24 bits cut into four groups of 6 bits are 30, 50, 9 and 33, the characters e, y, J and h. That is why every token starts with eyJ. The header
becomes 68 characters and the claims 191. The signing input is the two parts joined by a dot, 260 ASCII bytes. Ed25519 signs it with the bank's private
key into 64 bytes, 86 characters of base64url. The token is 68 + 1 + 191 + 1 + 86 = 347 characters.

### 2. Decoding is not verifying

Only the claims part is replaced: "sub":"ana" becomes "sub":"mallory", so part 2 grows from 191 to 196 characters, while the header and Ana's
86-character signature stay as they were. A reader that only decodes splits the token at the dots, decodes part 2, reads sub and says mallory. The
validator first verifies part 3 over parts 1 and 2 with the public key of bank-2026-01: false, because the signature was made over Ana's claims. It refuses
the token: the signature does not match. Anyone can read the claims, since base64url is an encoding and not encryption, so no secret belongs in them.

### 3. The server chooses the algorithm, never the token

The API's settings are fixed in Policy: algorithm EdDSA, type at+jwt. The Ed25519 code is first checked against the published example of RFC 8037,
appendix A: it verifies with the published public key, signing it again gives the same 64 bytes, and the public key derived from the private key matches.
Four headers are sent, all with Ana's claims. EdDSA with at+jwt, Ana's real token, is accepted. none with no signature is refused (algorithm none is not
EdDSA); HS256 is refused (algorithm HS256 is not EdDSA); an ID token, typ id+jwt, offered as an access token is refused (type id+jwt is not at+jwt).
The header is only checked against the API's own settings.

### 4. Every claim that matters is checked

The API expects the issuer https://auth.bank.example, the audience accounts-api and the scope accounts:read, with 30 seconds of clock skew. Ana's token
has iat and nbf 1800000000 and exp 1800000300, so it is accepted while now is before exp + 30 = 1800000330. At 1800000000, the moment of issue, it is
accepted; at 1800000320 (exp + 20) it is still accepted; at 1800000330 (exp + 30) it is refused as expired. Then one claim is changed per token, each
signed by the bank: nbf 1800000120 (it starts in 2 minutes) is refused as not valid yet, iss https://auth.partner.example as another issuer, aud
loyalty-api as another audience, and scope cards:read as missing scope accounts:read. The signature proves who wrote the token; only the claims say
whether it is for this API, now.

### 5. Keys by id, from a set the API already holds

The API holds a key set: each key id (kid) with its public key x, bank-2026-01 (the RFC 8037 test key) and bank-2026-07. During the changeover a token
with kid bank-2026-01 signed with bank-2026-01 and one with kid bank-2026-07 signed with bank-2026-07 are both accepted. After bank-2026-01 is retired, the
set holds only bank-2026-07: the old token is refused (unknown key id bank-2026-01), a token naming attacker-key is refused (unknown key id attacker-key),
and a token signed with an intruder's key but naming bank-2026-07 is refused (signature does not match). The API never fetches a key because a token
asked it to.

### 6. The bill: a token cannot be taken back

Ana holds an access token, which stops working at 300 + 30 of skew = 330 seconds after issue, and a refresh token rt-1, which the bank looks up each time.
At 120 seconds the bank removes Ana's access and revokes rt-1. At 240 the access token is still accepted: it still works, because the API checks the
token, not the bank's records. The refresh token is refused as revoked. At 330 the access token is refused as expired. So the policy allows at most 900
seconds, 15 minutes, and a token with exp = iat + 3600 is refused: lifetime longer than the policy allows. In production, use a maintained library,
configured strictly.

## The verdict

Use a maintained library, configured strictly: one allowed algorithm, an explicit token type, a fixed issuer and audience, a required expiry with a small
skew, a key set loaded from configuration or from the issuer's own JWKS address, never from the token. Keep access tokens short and refresh tokens on the
server. Never put secrets in claims.

## How to recognise this in code you did not write

- `Authorization: Bearer eyJ...` in API requests.
- A JWKS document at `/.well-known/jwks.json` with `kid` values.
- Library settings named `allowedAlgorithms`, `expectedAudience`, `clockSkew`.
- Advisories titled "JWT signature bypass" or "algorithm confusion".

## Where you have already met this

- `Authorization: Bearer eyJ...` headers in API calls; every token starting with `eyJ` is base64url of `{"`.
- OAuth 2.0 and OpenID Connect providers such as Keycloak, and their `/.well-known/jwks.json` key sets.
- Debugging sites that decode a JWT: they show the claims without checking anything, which is exactly the point of act two.
- Advisories about `alg: none` and algorithm confusion in JWT libraries.
