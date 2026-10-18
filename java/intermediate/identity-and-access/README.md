# Identity and access

Modern authentication and authorization: tokens, OAuth 2.0 and OpenID Connect as the current best practice (RFC 9700) describes them, keeping tokens out of the browser, passkeys, and legacy single sign-on.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [jwt-done-right](jwt-done-right) | A JWT validator that fixes the algorithm, checks expiry, issuer, audience, scope and type, chooses keys by id from its own set, and keeps lifetimes short | A07, CWE-347, RFC 8725 |
| 2 | [oauth2-authorization-code-pkce](oauth2-authorization-code-pkce) | The authorization code flow step by step, PKCE against the RFC 7636 example, exact redirect URIs, state, single-use codes, scopes and rotated refresh tokens | RFC 6749, RFC 7636, RFC 9700, A07 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
