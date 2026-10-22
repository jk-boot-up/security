# Problem Statement

## The scenario

An online bank on Spring Boot with a session-cookie login, where a developer sees a 403 on a POST and wants it gone.

## The naive version

Disable CSRF protection, or exempt the route that fails, and move on.

## What this project must deliver

- Real Spring Boot servers in four configurations.
- A browser stand-in that sends a forged post the way another site's page would.
- The default token, the single-page cookie mode, exemptions and GET.
- A separate API chain without CSRF, and a check of every state-changing route.
