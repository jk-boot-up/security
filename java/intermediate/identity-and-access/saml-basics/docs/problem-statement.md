# Problem Statement

## The scenario

A shop lets customers sign in through their bank using SAML. The bank's identity provider sends a signed XML response to the shop through the customer's browser.

## The naive version

Check that the response has a valid signature, then read the user's name from the assertion.

## What this project must deliver

- An identity provider that signs SAML-shaped responses with the JDK's XML signature API.
- Three service providers: trusting the embedded key, pinning the key but reading the wrong element, and strict.
- A self-signed response, an edited one, a wrapped one, and stale, replayed and misdirected ones.
