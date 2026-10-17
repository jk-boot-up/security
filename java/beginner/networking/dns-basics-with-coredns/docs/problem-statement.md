# Problem Statement

## The scenario

An online store serves its own zone and needs to know what that server does, what it will answer and what it cannot protect against.

## The naive version

Assume that a DNS answer is true because it came from the store's own server.

## What this project must deliver

- A real CoreDNS server started from a Corefile and a zone file, with pinned image, bound to loopback.
- Queries over UDP and TCP written and read by the project's own DNS message code.
- NXDOMAIN, REFUSED and a rewrite shown.
- A cache whose time to live counts down.
- A wrong record served like a right one.
- Containers always stopped and removed, with a test that checks.
