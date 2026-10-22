# Problem Statement

## The scenario

A shop lets shoppers sign in through their bank's SAML identity provider, and has to decide which XML to believe.

## The naive version

Accept any response that has a valid XML signature and read the first assertion.

## What this project must deliver

- A real Keycloak SAML identity provider with a bank and a wallet realm.
- A Spring Security service provider and its refusal codes.
- The attacker's edits and signatures, done with the JDK's XML tools.
- A key rotation that locks the shop out.
