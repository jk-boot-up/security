# Problem Statement

## The scenario

An online bank wants customers to connect a budgeting app that reads their accounts. The app is written by another company, and the bank must not let it see
the customer's password, move money, or keep access for ever.

## The naive version

Ask the customer for the bank password inside the app, or send the access token straight back through the browser's address bar, and match redirect
addresses by prefix so every environment works.

## What this project must deliver

- An authorization server with the authorization and token endpoints, built with the JDK.
- PKCE with S256 only, checked against RFC 7636 appendix B.
- Exact redirect URI matching, with errors shown at the bank instead of redirected.
- A state bound to the browser session, single-use codes that expire after 60 seconds, and revocation when a code is reused.
- Scopes enforced by the bank's API, and refresh token rotation with reuse detection.
- The whole flow over a real local HTTP server.
