# Spec — An HTTPS Server with Spring Boot

## Purpose

Teach how a framework turns TLS decisions into configuration by running real Spring Boot servers with an SSL bundle, a redirect, HSTS, secure cookies, certificate reload and mutual TLS, and clients that verify them.

## Not in scope

- Running behind a proxy or load balancer.
- A real certificate authority, or an ACME client such as Let's Encrypt.
- User authentication on top of TLS.

## The problem

Learners can switch TLS on with a few properties without knowing what they have and have not protected.

## The pieces

| Role | Class |
| --- | --- |
| A running store on free ports | `Shop` |
| The application and its security | `ShopApp` |
| A verifying client | `ShopClient`, `Reply` |
| Demo keystores | `Certificates` |
| Entry point | `HttpsSpringDemo` |

## Requirements

1. The server speaks TLS 1.3 only, from an SSL bundle.
2. Clients refuse a wrong name, an old TLS version and an untrusted certificate.
3. Plain HTTP is redirected without HSTS, and HTTPS answers carry HSTS and a secure cookie.
4. A renewed certificate is served without a restart, only when reload is on.
5. With client-auth need, only a certificate signed by the root is accepted.
6. The demo prints the same output on every run although the ports are random.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 22, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 08:06, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
