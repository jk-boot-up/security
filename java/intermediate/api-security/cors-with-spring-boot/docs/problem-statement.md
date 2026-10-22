# Problem Statement

## The scenario

A bank API on Spring Boot that a partner's web page must call from a browser, where a developer sees a CORS error and wants it gone.

## The naive version

Reflect the Origin header, or allow every pattern, until the error goes away.

## What this project must deliver

- Real Spring Boot servers with six different CORS policies.
- A browser stand-in that applies the browser's rules to the real headers.
- The mistakes: a reflecting pattern, null, a lookalike and a late registration.
- A test for every origin that matters.
