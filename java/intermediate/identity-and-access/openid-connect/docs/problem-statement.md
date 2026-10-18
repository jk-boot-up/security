# Problem Statement

## The scenario

An online store wants customers to sign in with their bank instead of creating another password. Tokens arrive through the customer's browser, from
providers the store must recognise, for a customer the store must identify correctly every time.

## The naive version

Use the OAuth access token as proof of sign-in, decode the ID token without checking its issuer, audience or nonce, fetch keys from wherever the token
or a document says, and find the customer's account by email address.

## What this project must deliver

- A relying party that binds state and nonce to the browser and redeems the code server to server.
- ID token validation as OpenID Connect Core 1.0 section 3.1.3.7 requires, with every refusal tested.
- Discovery from one configured issuer, with the mix-up check and keys only from the issuer's own site.
- Key rotation by key id, through a reload of the provider's key set.
- Accounts keyed by issuer and subject, and unverified email never used.
- RP-initiated logout and back-channel logout tokens, validated.
