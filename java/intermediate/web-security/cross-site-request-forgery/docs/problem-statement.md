# Problem Statement

## The scenario

An online bank lets a logged-in customer move money with a form. The customer's browser also visits other sites, and any of them can make the browser send requests to the bank.

## The naive version

Trust the session cookie: if the request carries it, the customer must have sent it.

## What this project must deliver

- A bank on a real local server whose transfer can be forged from another site's page.
- Per-session form tokens, compared in constant time.
- SameSite cookies, enforced by a browser model that follows real browsers' rules.
- Fetch Metadata and Origin checks, and a bank that refuses state-changing GET requests.
- The limits of each defence: older browsers, same-site subdomains, forgotten tokens, and why bearer-token APIs differ.
