# JWT Done Right with Nimbus JOSE + JWT, Explained

## The idea in one sentence

A standard library verifies the signature and most claims, and the API still decides the algorithm, the type, where keys come from, the scope and the lifetime.

## The 5 acts

### 1. Decoding is not verifying

The bank issues ana an access token of 347 characters in 3 parts. The header says ES256, type at+jwt and key id bank-2026-01. The claims name the issuer, the subject ana, the audience accounts-api, the scope, and the times. The third part is a 64-byte ES256 signature. The claims are readable by anyone: base64url is an encoding, not encryption. With the claims edited to sub mallory and ana's signature kept, a reader that only parses says mallory, and the validator refuses it: Invalid signature.

### 2. The server chooses the algorithm

Ana's token with the bank's key is accepted. The same claims with a header that says none and no signature are refused: Unsecured JWTs are rejected, even though the header names the right type. A token whose header says HS256, signed with the bank's public key as the shared secret, is refused: another algorithm was expected. A correctly signed token of type id+jwt, offered as an access token, is refused: type not allowed. The algorithm and the type are settings of the API.

### 3. Every claim that matters

At the moment of issue the token is accepted. 20 seconds after exp, inside the 30 seconds of clock skew, it is accepted. 31 seconds after exp it is refused: Expired JWT. A token that starts in 2 minutes is refused: before use time. A token from another authorization server is refused for its issuer, and one meant for the loyalty API is refused for its audience. A token with only the scope cards:read is refused for a missing scope accounts:read. A valid signature proves who wrote the token; only the claims say whether it is for this API, now.

### 4. Keys by id, from one URL

The bank publishes 1 key. The first token fetches the key set and 100 more tokens come from the cache: the endpoint was asked 1 time. A token naming a key id the API has never seen makes the library ask once more and is refused, and 100 more of them cause 0 more fetches, because of the minimum time between refetches. A token signed with another key under the bank's key id is refused for its signature. A token whose header points at the attacker's own key set is refused and the attacker's server is asked 0 times, and a token that carries its own public key is refused too. The bank publishes a new key before using it; after the cache time the API trusts 2 keys. When the old key is retired, the old token is still accepted inside the cache time, and refused after it.

### 5. The bill: a token cannot be taken back

Ana's access is removed 2 minutes after her token was issued. Her access token is still accepted: it works until it expires. Her refresh token, which the server looks up, is refused as revoked. At 5 minutes and 31 seconds the access token is refused: Expired JWT. A token that asks to live for 24 hours is refused by the lifetime policy of 15 minutes, which is one of our own checks. The library does the parsing, the signature and the claims, and the policy stays ours.

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
