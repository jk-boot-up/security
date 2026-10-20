# Problem Statement

## The scenario

A bank's API serves its own pages and a partner's page, in browsers where an attacker's page and a look-alike site can also run scripts.

## The naive version

When the browser blocks a cross-origin call, add the CORS header that makes the error go away: reflect the origin, allow null for local testing, match the end of the domain.

## What this project must deliver

- A bank API with account, rates and transfer calls and pluggable CORS policies.
- A browser model with simple requests, preflight, credentials and the opaque null origin.
- Three unsafe policies, each with the attacker who benefits.
- An exact allow-list with `Vary: Origin` and tight preflight answers.
