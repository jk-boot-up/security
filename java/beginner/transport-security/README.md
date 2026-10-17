# Transport security

Protecting the connection: how a secure channel is agreed, who vouches for a key, and how a server is put together so that all of it is switched on.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [tls-handshake](tls-handshake) | A real TLS 1.3 handshake between two engines in memory, what a sniffer sees, and what is refused |
| 2 | [tls-handshake-with-openssl](tls-handshake-with-openssl) | OpenSSL as the other end of the handshake: protocol, cipher suite, host name and downgrade checks |
| 3 | [certificates-and-chain-of-trust](certificates-and-chain-of-trust) | Root, intermediate and leaf certificates built by hand and validated with the JDK, with expiry, revocation and name checks |
| 4 | [certificates-and-chain-of-trust-with-bouncy-castle](certificates-and-chain-of-trust-with-bouncy-castle) | The same chains built with Bouncy Castle extensions, name constraints and CRLs, and checked by OpenSSL |
| 5 | [https-server](https-server) | An HTTPS server with redirect, HSTS and secure cookies, a client that checks names, and a configuration checklist |
| 6 | [https-server-with-spring-boot](https-server-with-spring-boot) | SSL bundles, certificate reload and mutual TLS in Spring Boot 4 |
| 7 | [https-server-with-nginx](https-server-with-nginx) | TLS termination in nginx, the forwarded-protocol trap and a configuration score, in Docker |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [learning path](../learning-path.md) for the order of all projects.
