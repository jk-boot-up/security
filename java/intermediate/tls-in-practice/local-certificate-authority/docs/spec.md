# Spec — A Local Certificate Authority

## Purpose

Teach how an internal certificate authority issues, constrains and revokes certificates, and why short-lived certificates beat revocation.

## Not in scope

- A production CA, an HSM or an ACME server (the -with-step-ca version covers the tooling).
- OCSP, certificate transparency and the public web PKI.
- Cross-signing and multiple roots.

## The problem

Teams issue certificates by hand with no policy, long lifetimes and no plan for taking one back.

## The pieces

| Role | Class |
| --- | --- |
| The authority and its policy | `CertificateAuthority`, `Profile`, `RefusedException` |
| Requests and keys | `Csr`, `Keys`, `Issued` |
| Clients | `Validator` |
| Lifetime | `ShortLived`, `Inventory` |
| Entry point | `LocalCaDemo` |

## Requirements

1. A request must prove possession of the key it carries.
2. The issuer enforces names, allow-lists and the profile's maximum lifetime.
3. The issuing authority cannot create authorities and cannot name anything outside shop.internal.
4. A revoked certificate is refused by clients that check a current list, and an out-of-date list fails closed.
5. A short-lived certificate renews at two thirds of its life with a new key and expires if the job stops.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 16, all passing |
| Demo output | 39 lines, identical on every run |
| Acts | 5 |
| Video | 06:58, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
