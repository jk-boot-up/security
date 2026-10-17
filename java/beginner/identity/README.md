# Identity

Who is asking, and what may they do? This module separates the two questions that every login system answers, and shows the 401 and 403 answers that go with them.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [authentication-vs-authorization](authentication-vs-authorization) | An online store with no checks, a login only, and a login with a policy of roles and ownership; sessions, expiry and stale roles |
| 2 | [authentication-vs-authorization-with-spring-security](authentication-vs-authorization-with-spring-security) | The same story through Spring Security's real filter chain, BCrypt, @PreAuthorize rules and a forgotten rule |
| 3 | [authentication-vs-authorization-with-keycloak](authentication-vs-authorization-with-keycloak) | The same lesson against a real Keycloak login server in Docker: tokens, JWT validation, audience, and roles that go stale until a token expires |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

The shared decisions are in [`docs/spec.md`](docs/spec.md) and [`docs/implementation-plan.md`](docs/implementation-plan.md).
