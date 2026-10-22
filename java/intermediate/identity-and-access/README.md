# Identity and access

Modern authentication and authorization: tokens, OAuth 2.0 and OpenID Connect as the current best practice (RFC 9700) describes them, keeping tokens out of the browser, passkeys, and legacy single sign-on.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [jwt-done-right](jwt-done-right) | A JWT validator that fixes the algorithm, checks expiry, issuer, audience, scope and type, chooses keys by id from its own set, and keeps lifetimes short | A07, CWE-347, RFC 8725 |
| 2 | [oauth2-authorization-code-pkce](oauth2-authorization-code-pkce) | The authorization code flow step by step, PKCE against the RFC 7636 example, exact redirect URIs, state, single-use codes, scopes and rotated refresh tokens | RFC 6749, RFC 7636, RFC 9700, A07 |
| 3 | [passkeys](passkeys) | A model WebAuthn bank: registration, single-use challenges, phishing resistance by site name and origin, user verification for payments, counters and recovery | A07, NIST SP 800-63B, WebAuthn |
| 4 | [tokens-in-the-browser](tokens-in-the-browser) | A token a script can read is a token an attacker can take | A07 |
| 5 | [openid-connect](openid-connect) | Access is not identity | A07, RFC 9700 |
| 6 | [saml-basics](saml-basics) | A valid signature is not the same as the statement you believed | A07, A08, CWE-347, CWE-345 |
| 7 | [jwt-done-right-with-nimbus-jose-jwt](jwt-done-right-with-nimbus-jose-jwt) | A standard library does the parsing, the signature and most claims; the algorithm, the type, the key set URL and the policy are still the API's to set | A07, CWE-347 |
| 8 | [oauth2-authorization-code-pkce-with-keycloak](oauth2-authorization-code-pkce-with-keycloak) | The same flow against a real authorization server: the checks that make it safe are settings on the client, and Keycloak enforces them | RFC 9700, A07 |
| 9 | [openid-connect-with-spring-security-and-keycloak](openid-connect-with-spring-security-and-keycloak) | Spring Security checks the ID token and keeps the state and nonce; what it leaves to the store is who the customer is, and how sign-out reaches every session | A07 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
