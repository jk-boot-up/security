# Problem Statement

## The scenario

A budgeting app must read a bank customer's accounts without ever holding the customer's password, and the bank must be able to stop it at any time.

## The naive version

Ask for the password, or use the implicit flow, accept any redirect address that looks like the app's, and trust the code.

## What this project must deliver

- A real Keycloak in Docker with the bank realm imported from a file.
- A Spring Security resource server that checks signature, issuer, audience, dates and scope.
- A browser and app stand-in that drive the real flow, and the mistakes around it.
- A realm file whose settings are checked by tests.
