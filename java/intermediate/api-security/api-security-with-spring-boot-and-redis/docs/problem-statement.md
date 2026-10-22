# Problem Statement

## The scenario

A bank API on Spring Boot, first written the quick way, with controllers that take and return the stored record and nothing that counts requests.

## The naive version

Bind the body to the entity, return the entity, and trust the callers to behave.

## What this project must deliver

- Real Spring Boot servers in a first version and a second version.
- A request record, a response record, validation and a page ceiling.
- A token bucket per caller in Redis, in one Lua script.
- Two servers sharing one budget, and an API that refuses when the counter is down.
