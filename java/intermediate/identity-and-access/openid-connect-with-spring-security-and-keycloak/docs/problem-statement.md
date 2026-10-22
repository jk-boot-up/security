# Problem Statement

## The scenario

An online store wants shoppers to sign in with a login they already have, at a bank and at a wallet, and to end every session when they sign out anywhere.

## The naive version

Take the e-mail address from the token as the customer's identity, and sign out only at the store.

## What this project must deliver

- A real Keycloak with two realms and a real Spring Security 7 client.
- Spring Security's own ID token checks, shown refusing crafted tokens.
- An account takeover through an unverified e-mail address, and the fix.
- RP-initiated and back-channel logout, working end to end.
