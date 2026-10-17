# Problem Statement

## The scenario

An online bank's accounts API receives access tokens from customers' apps. The tokens were issued by the bank's authorization server, but the API
receives them from the customer, so every byte of a token arrives from someone who may have changed it.

## The naive version

Split the token, decode the claims, check that it has not expired, and use the subject. Or use a library with its defaults, which may take the algorithm
from the header, accept any audience and fetch a key from a URL in the token.

## What this project must deliver

- A strict validator: three parts, base64url and JSON read strictly, duplicate members refused.
- An algorithm and token type fixed by the API, so `none`, `HS256` and an ID token are refused.
- Ed25519 verification with the JDK, checked against RFC 8037.
- Expiry with a small skew, not-before, issued-at, maximum lifetime, issuer, audience and scope.
- Keys chosen by key id from a set the API holds, with rotation and no fetching.
- Short lifetimes, and refresh tokens kept on the server so that access can be withdrawn.
