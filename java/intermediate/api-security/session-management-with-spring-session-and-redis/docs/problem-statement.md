# Problem Statement

## The scenario

An online bank that outgrows one server and moves its sessions into Redis, where every server can reach them.

## The naive version

Add the dependency, point it at a Redis, and assume that the session rules came with it.

## What this project must deliver

- Real Spring Boot servers and a real, password-protected Redis in Docker.
- A new id at sign-in, an idle limit, an absolute limit, and server-side sign-out.
- Sign-out everywhere, a password change that ends other sessions, and a session limit.
- A bank that refuses when Redis does not answer.
