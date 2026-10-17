# JWT Done Right, Explained

## The idea in one sentence

A JWT is believable only after the server has checked it against its own settings: its algorithm and type, a key it already holds, the signature, and
then the expiry, issuer, audience and scope; and since it cannot be taken back, it must expire soon.

## The 5 acts

### 1. Three parts, and decoding is not verifying

The bank's authorization server issues Ana an access token of 347 characters in 3 parts. The header says the algorithm is EdDSA, the type is at+jwt
and the key id is bank-2026-01. The claims name the issuer, the subject ana, the audience accounts-api, the scope accounts:read, and the times of issue,
start and expiry. The third part is a 64-byte Ed25519 signature over the first two. Anyone can read the claims, because base64url is an encoding, not
encryption. When the claims are edited to sub=mallory and Ana's signature is kept, a reader that only decodes says mallory. The validator refuses it:
the signature does not match.

### 2. The server chooses the algorithm, never the token

The published RFC 8037 example token verifies with its published key, signing it again gives the same signature, and the JDK derives the published
public key from the published private key. Ana's token is accepted with the bank's key. The same claims with a header that says none and no signature
are refused: the algorithm none is not EdDSA. A header that says HS256 is refused the same way. A correctly signed token of another type, an ID token
offered as an access token, is refused because its type id+jwt is not at+jwt. The algorithm and the type are settings of the API; the header is only
checked against them.

### 3. Every claim that matters is checked

At the moment of issue the token is accepted. Twenty seconds after its expiry time it is still accepted, inside the 30 seconds allowed for clock skew;
30 seconds after, it is refused as expired. A token that starts in 2 minutes, presented now, is refused as not valid yet. A token issued by another
authorization server is refused, and so is one meant for the loyalty API but sent to the accounts API, and one with only the scope cards:read. A valid
signature proves who wrote the token; only the claims say whether it is for this API, now.

### 4. Keys by id, from a set the API already holds

During the changeover the API trusts 2 keys, bank-2026-01 and bank-2026-07, and tokens signed with either are accepted. After the old key is retired,
the old token is refused with an unknown key id. A token naming a key id the API has never seen is refused, and so is a token signed with another key but
naming the bank's key id, because its signature does not match. An unknown key id is refused; the API never fetches a key because a token asked it to.

### 5. The bill: a token cannot be taken back

Ana's access is removed 2 minutes after her token was issued. Two minutes later her access token is still accepted: it still works until it expires.
Her refresh token, which the server looks up every time, is refused as revoked. At 5 minutes and 30 seconds the access token is refused as expired. So
access tokens live for minutes, and a token asking for more than 15 minutes is refused by policy. Where a browser should keep tokens is the subject of
the tokens-in-the-browser project, and in production a maintained library should do this work.

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
- Debugging sites that decode a JWT: they show the claims without checking anything, which is exactly the point of act one.
- Advisories about `alg: none` and algorithm confusion in JWT libraries.
