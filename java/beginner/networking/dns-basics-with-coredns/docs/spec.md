# Spec — DNS Basics with CoreDNS

## Purpose

Teach how a real DNS server behaves by running CoreDNS in a container and asking it questions over UDP and TCP with the project's own DNS message code, then showing missing names, a rewrite, a counting cache and a wrong record.

## Not in scope

- Running a production DNS service.
- Recursive resolution and forwarding in depth.
- DNSSEC, which is described and not run.

## The problem

Learners see DNS as a lookup that just works, and need to see that a server serves what a file says.

## The pieces

| Role | Class |
| --- | --- |
| CoreDNS container | `CoreDns` |
| Corefile and zone text | `ShopZone` |
| UDP and TCP client | `Client` |
| DNS messages | `DnsMessage` |
| Entry point | `CoreDnsDemo` |

## Requirements

1. The image is pinned to an exact version and the server is bound to loopback.
2. The client reads answers over UDP and over TCP with the two-byte length.
3. NXDOMAIN, REFUSED and the rewrite are shown.
4. Cached answers count down.
5. A wrong record is served as any other.
6. Containers never leak, and the demo prints only facts that are the same on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 24, all passing |
| Demo output | 27 lines, identical on every run |
| Acts | 5 |
| Video | 07:09, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
