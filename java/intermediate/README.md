# Intermediate: Applied Security (Java)

One self-contained Java 25 + Gradle project per concept, as in the [beginner track](../beginner). Where the beginner track explained each
control, this track attacks a realistic online store or bank and fixes it the way production code does. Topics follow OWASP Top 10 (2025),
the OWASP API Security Top 10, ASVS 5.0, the CWE Top 25, OAuth 2.0 security best practice (RFC 9700), NIST SSDF and SLSA.

| Module | Covers |
| --- | --- |
| [web-security](web-security) | access control, injection, XSS, CSRF, SSRF, deserialization, file handling |
| [secure-configuration](secure-configuration) | misconfiguration, failing closed |
| [api-security](api-security) | API Top 10, sessions, CORS, webhooks |
| [identity-and-access](identity-and-access) | JWT, OAuth 2.0 with PKCE, OpenID Connect, tokens in the browser, passkeys, SAML |
| [tls-in-practice](tls-in-practice) | a local CA, TLS servers, misconfigurations, traffic inspection, pinning |
| [secure-coding](secure-coding) | validation, race conditions, secrets in logs, fuzzing |
| [secrets-and-supply-chain](secrets-and-supply-chain) | secrets management, signing, SBOMs and provenance, dependency confusion |
| platform-hardening | Linux and container hardening |
| [detection-and-response](detection-and-response) | security logging, detection, incident response |
| [secure-design](secure-design) | threat modeling, attack trees and abuse cases, ASVS requirements |

The full plan, with every project and the references it maps to, is in [`../../docs/intermediate-plan.md`](../../docs/intermediate-plan.md).
Run a project as in the beginner track: `./gradlew test`, then `./gradlew run`.
