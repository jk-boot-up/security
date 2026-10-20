# Problem Statement

## The scenario

An online store lets sellers import a product image from a web address. The server fetches the address from inside the company network.

## The naive version

Fetch the address the seller gives: it is only a picture, so any address is fine.

## What this project must deliver

- A simulated network with a public host, an attacker's host, the metadata address, the server itself and an internal page.
- Three importers: no check, a blocklist of names, and one that checks the real address.
- An address policy that refuses loopback, link-local, private, shared and mapped ranges.
- A demonstration of DNS rebinding and of why the checked address must be the connected address.
