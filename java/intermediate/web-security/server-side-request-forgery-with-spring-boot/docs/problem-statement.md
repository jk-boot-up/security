# Problem Statement

## The scenario

An online store imports product images from addresses that staff paste into a form, from a server inside the company network.

## The naive version

Fetch the address with RestClient, perhaps after refusing a few host names.

## What this project must deliver

- A real Spring Boot shop and Spring's RestClient.
- An invented internet of real loopback servers: public, attacker, metadata, localhost and office.
- A blocklist importer that five disguises beat, and a guarded importer that resolves, judges, pins and limits.
- A rebinding name server and a check-then-connect importer that it beats.
