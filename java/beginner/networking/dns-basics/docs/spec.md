# Spec — DNS Basics for Security

## Purpose

Teach how names become addresses and how that answer can be forged, by encoding and decoding DNS messages, simulating a caching resolver, running a real UDP exchange, and poisoning a cache by guessing a transaction id.

## Not in scope

- A full resolver or an authoritative server with all record types.
- DNSSEC validation: it is described, not run.
- A real DNS server and the tool counterpart: a later project.

## The problem

Learners use names every day and trust the address they get, without knowing that the answer is unsigned.

## The pieces

| Role | Class |
| --- | --- |
| The message codec | `DnsMessage` |
| Where ids and ports come from | `TransactionIds` |
| The caching resolver | `Resolver` |
| A real UDP exchange | `LoopbackExchange` |
| Entry point | `DnsDemo` |

## Requirements

1. A query matches the RFC 1035 layout and an answer's name is a pointer.
2. Messages round-trip, and pointer loops, cut messages and bad names are refused.
3. The resolver believes only a response that repeats id, port and question, and caps lifetimes.
4. Counting ids are guessed and keyed ids and ports are not.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 26 lines, identical on every run |
| Acts | 5 |
| Video | 07:46, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
