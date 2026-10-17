# Problem Statement

## The scenario

An online store is reached by name. Every customer's browser asks a resolver for the address, and the resolver caches the answer for everyone who asks after.

## The naive version

Assume that an answer to a DNS question came from the owner of the name, and that the resolver's cache is as trustworthy as the store.

## What this project must deliver

- A DNS message built and read byte by byte, with RFC 1035 name compression.
- A caching resolver with a time to live and a simulated clock.
- A real question and answer over UDP on this machine.
- Cache poisoning against counting ids, and its failure against keyed ids and ports.
- A cap on lifetimes, and the roles of DNSSEC, encrypted DNS and TLS.
