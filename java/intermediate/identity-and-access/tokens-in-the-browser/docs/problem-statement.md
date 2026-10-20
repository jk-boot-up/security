# Problem Statement

## The scenario

A single-page banking app gets an access token after sign-in, and must keep it somewhere between API calls.

## The naive version

Put the token in local storage, because it is easy and survives a reload: our own scripts are the only ones on the page.

## What this project must deliver

- A model of a page, its storage and cookies, and what an injected script can reach.
- A BFF with a session cookie, an origin check and a custom header.
- Real DPoP proofs (ES256, JWK thumbprints) and an API that checks them, with a replay cache.
- An honest account of what each design does not stop.
