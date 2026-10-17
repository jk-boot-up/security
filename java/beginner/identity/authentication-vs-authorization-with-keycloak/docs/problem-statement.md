# Problem Statement

## The scenario

An online store has a web front end, a nightly batch job and staff tools, all calling one order API. Customers, support staff and an admin must each be able to do only their part, and the shop does not want to store passwords.

## The naive version

Each application checks passwords itself, or the API accepts any token that looks like a token. Passwords spread to many places, and tokens are trusted without checking who issued them or whom they are for.

## What this project must deliver

- An order API that accepts only tokens signed by the login server's key.
- 401 for a missing, forged, tampered or expired token, and 403 for a valid token without the right role.
- Roles read from realm_access.roles and mapped to authorities.
- Issuer and audience checks, and a demonstration of what happens without the audience check.
- The stale-role problem and the short-lifetime control, and a revocation list with its cost.
- A realm file for Keycloak that is itself tested: public client with PKCE, no password grant, exact redirect addresses, short tokens, brute force protection.
- Integration tests against a real Keycloak 26.8.1: login with PKCE, a service account, a removed role and a real expiry.
