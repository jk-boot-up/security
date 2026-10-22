# Problem Statement

## The scenario

An online bank's accounts API receives bearer tokens from many apps and must decide, for each one, whether it is genuine, meant for this API, and still valid.

## The naive version

Parse the token, read the claims and trust what they say, or let the token say how it should be checked.

## What this project must deliver

- A real key set endpoint and ES256 access tokens.
- A validator built on Nimbus JOSE + JWT with a fixed algorithm and type.
- A key cache, a refetch limit and a rotation that is published first.
- Our own checks for the scope and the lifetime.
