# Problem Statement

## The scenario

An online bank has a single-page app that calls its accounts API with OAuth tokens, and a page that also loads third-party scripts.

## The naive version

Keep the tokens in localStorage so the app can add them to every call.

## What this project must deliver

- A real Keycloak, accounts API and Spring Boot BFF.
- A script on the page, and what it can take in each design.
- Cookies with their attributes, an XSRF header and a refused preflight.
- DPoP-bound tokens and proofs checked by Spring Security.
