# Problem Statement

## The scenario

An online store has several applications that need a payment key and a database login, and every one of them keeps its own copy.

## The naive version

Put the secrets in the code, the environment or the image, shared by every application and never changed.

## What this project must deliver

- A real OpenBao and a real PostgreSQL in Docker.
- AppRole logins, policies and versioned secrets.
- Leased database logins that really stop.
- Rotation, revocation and a sealed server.
