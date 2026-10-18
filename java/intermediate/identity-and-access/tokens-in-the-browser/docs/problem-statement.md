# Problem Statement

## The scenario

An online bank builds its banking app as a single page that calls the bank's API, and signs customers in with OAuth. The app includes third-party scripts for
analytics and chat. Any one of them could be compromised.

## The naive version

Keep the access token and the refresh token in localStorage, and send the access token as a bearer token with every request. It works, and every script on the
page can read both tokens.

## What this project must deliver

- A model browser that applies what scripts can read, SameSite and the CORS preflight.
- A demonstration that a script on the page takes tokens kept in the browser.
- A backend for the frontend that keeps the tokens on the server and gives the browser a __Host- cookie.
- Requests from another site stopped by SameSite and a request header.
- DPoP proofs created and checked, with the JWK thumbprint matching RFC 8037.
- The costs, each shown with a number.
