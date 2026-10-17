# Intermediate track plan: Applied Security (Java)

The beginner track explained what each control is. The intermediate track attacks a realistic online store or bank application the
way real applications are attacked, then fixes it the way production code fixes it. The topics follow the references that the industry
uses to build and test applications, not a fixed list:

| Reference | What it is | Where it shapes this track |
| --- | --- | --- |
| OWASP Top 10 (2025) | The ten most common and serious web application risks | Modules 1, 2, 6, 7, 9 |
| OWASP API Security Top 10 (2023) | The equivalent list for APIs | Module 3 |
| OWASP ASVS 5.0 | A verification standard: what a secure application must do, by level | Every project's "how it is checked" section names its ASVS requirements |
| CWE Top 25 | The most dangerous software weaknesses | Every project names its CWE ids |
| OAuth 2.0 Security Best Current Practice (RFC 9700), OAuth for browser-based apps | How OAuth is used safely today | Module 4 |
| NIST SSDF (SP 800-218), SLSA | Secure development practices and supply chain levels | Modules 7 and 9 |

Each project's README says which of these it maps to, so a learner can connect the lesson with an audit or a pen-test report.

## Conventions (unchanged from the beginner track)

- One self-contained Java 25 + Gradle project per concept, under `java/intermediate/<module>/<project>/`, with `level = "intermediate"` in its `concept.toml`.
- Each project has a README, the five docs, diagrams, a threat model with a diagram, a five-act demo with a golden output, an animation and a narrated video.
- A plain version uses the JDK. An `-with-<tool>` sibling uses the framework or tool that real systems use, and comes straight after it.
- The domain is an online store or a bank, whichever fits.
- Every lab runs on the learner's machine, and every attack targets only the project's own deliberately weak code and containers, paired with the fix.
- Latest stable versions, pinned. No deprecated API: every build compiles with `-Xlint:deprecation -Xlint:removal -Werror`.
- Deterministic tests. Tool-dependent tests are tagged and run through `./gradlew integrationTest`, and they always remove their containers.
- Nothing is uploaded until the author has reviewed it.

**Frameworks and tools** (all local): Spring Boot 4.1 with Spring Framework 7 and Spring Security 7 on Tomcat; Hibernate 7; PostgreSQL, Redis,
Hazelcast, Ehcache; Vert.x; Jackson 3; Keycloak; nginx, Caddy, step-ca, OpenSSL; OpenBao (the open-source Vault fork); Sigstore cosign;
Syft and Grype; Jazzer (fuzzing); Wireshark/tshark; testssl.sh; OWASP ZAP; Grafana Loki, OpenSearch; pytm, OWASP Threat Dragon; Docker, kind.

## Modules and projects

### 1. web-security: injection and access control (OWASP A01, A05)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 1 | `broken-access-control` | Order numbers that anyone can walk through (IDOR), a forgotten admin route, and a role checked in the page but not on the server; deny by default and object-level checks | `-with-spring-security`: ownership in queries, method security, and tests that prove every route is covered | A01, API1, CWE-639, CWE-862 |
| 2 | `sql-injection` | A product search built by string concatenation leaks every order; prepared statements, allow-listed sort columns, and a least-privilege database user | `-with-hibernate-and-postgres`: JPQL, Criteria and native-query injection in Hibernate 7 against a real PostgreSQL | A05, CWE-89 |
| 3 | `cross-site-scripting` | Reflected, stored and DOM-style XSS in product reviews; contextual output encoding and a strict, nonce-based CSP | `-with-spring-boot`: Thymeleaf escaping, `th:utext` traps, and CSP nonces from Spring Security | A05, CWE-79 |
| 4 | `cross-site-request-forgery` | A forged transfer form at a bank; synchronizer tokens, SameSite cookies and Fetch Metadata headers | `-with-spring-security`: CSRF tokens, the SPA cookie mode, and what disabling CSRF costs | A01, CWE-352 |
| 5 | `server-side-request-forgery` | "Import a product image from a URL" reaches the metadata address and internal services; allow-lists, checking the address after DNS resolution, and DNS rebinding | `-with-spring-boot`: `RestClient` with a guarded resolver, no redirects and egress limits | A01, CWE-918 |
| 6 | `xml-and-template-injection` | XXE in an invoice upload, and server-side template injection in an e-mail template; secure parser settings and logic-less templates | – | A05, CWE-611, CWE-1336 |
| 7 | `insecure-deserialization` | A cart cookie of Java serialized objects runs a gadget chain; `ObjectInputFilter` allow-lists and data-only formats | `-with-jackson`: polymorphic typing traps in Jackson 3 | A08, CWE-502 |
| 8 | `path-traversal-and-file-upload` | Invoice downloads with `../`, uploads that overwrite files or carry scripts; canonical paths, generated names and storage outside the web root | `-with-spring-boot`: multipart limits, content checks and an open-redirect check on the "return to" link | A01, CWE-22, CWE-434, CWE-601 |

### 2. secure-configuration: misconfiguration and exceptional conditions (OWASP A02, A10)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 9 | `security-misconfiguration` | Default credentials, debug endpoints, directory listing, verbose errors and permissive defaults, found by a checker and fixed one by one | `-with-spring-boot-actuator`: exposed actuator endpoints, heap dumps and environment leaks, and a hardened profile | A02, CWE-16 |
| 10 | `fail-closed-error-handling` | A payment check that fails open when a service times out, an exception that skips an authorization step, and resources not released; failing closed, timeouts and safe defaults | `-with-spring-boot`: global error handling that never leaks, circuit breakers that fail closed | A10, CWE-636, CWE-755 |

### 3. api-security: APIs and sessions (OWASP API Top 10)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 11 | `api-security` | Mass assignment, excessive data exposure, unlimited page sizes and missing rate limits in a bank API; explicit request and response models, and budgets | `-with-spring-boot-and-redis`: validation, response shaping and a Redis-backed rate limiter | API3, API4, API6 |
| 12 | `session-management` | Session fixation, missing rotation after login, idle and absolute timeouts, and a logout that does not end the session | `-with-spring-session-and-redis`: sessions in Redis and limits on concurrent sessions | A07, CWE-384 |
| 13 | `cors` | Reflected origins, the `null` origin and credentials expose a bank API to any site; an exact allow-list | `-with-spring-boot`: Spring's CORS configuration, done right and done wrong | A02, CWE-942 |
| 14 | `webhook-security` | A payment provider's webhook that anyone can forge or replay; HMAC signatures, timestamps, replay windows and idempotency | – | API8, API10, CWE-345 |

### 4. identity-and-access: modern authentication and authorization (OWASP A07, RFC 9700)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 15 | `jwt-done-right` | `alg: none`, algorithm confusion, missing `aud` and `exp` checks, key ids, rotation, and why short lifetimes matter | `-with-nimbus-jose-jwt`: the same checks with a standard library and a JWKS endpoint | A07, CWE-347 |
| 16 | `oauth2-authorization-code-pkce` | The authorization code flow step by step: a stolen code with and without PKCE, exact redirect URIs, `state`, and why the implicit and password grants are gone | `-with-keycloak`: the same flow against a real authorization server | RFC 9700, A07 |
| 17 | `openid-connect` | ID tokens and access tokens, `nonce`, discovery, userinfo and logout | `-with-spring-security-and-keycloak`: OIDC login in Spring Boot, with back-channel logout | A07 |
| 18 | `tokens-in-the-browser` | Why a single-page app should not hold tokens: the backend-for-frontend pattern with a session cookie, and sender-constrained tokens (DPoP) | `-with-spring-boot`: a small BFF in front of the bank API | OAuth for browser-based apps, RFC 9449 |
| 19 | `passkeys` | WebAuthn explained on a model authenticator: challenges, origins, signature counters and phishing resistance | `-with-spring-security`: passkey registration and login with Spring Security's WebAuthn support | A07, NIST SP 800-63B |
| 20 | `saml-basics` | Assertions, signatures and signature-wrapping attacks on a small XML signer: why legacy single sign-on needs care | `-with-keycloak`: a SAML service provider against Keycloak | A07, CWE-347 |

### 5. tls-in-practice

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 21 | `local-certificate-authority` | A small CA in Java: issue, renew, revoke and publish a CRL, with short-lived certificates as the alternative to revocation | `-with-step-ca`: a local step-ca with ACME and automatic renewal | A04 |
| 22 | `tls-servers-in-practice` | – (tools only) | `-with-nginx-and-caddy`: the same shop behind nginx and Caddy, automatic HTTPS from the local CA, HSTS and OCSP stapling | A02, A04 |
| 23 | `tls-misconfigurations` | A checker for weak protocols, weak ciphers, missing names, expired and incomplete chains | `-with-testssl`: testssl.sh against deliberately misconfigured local servers | A02, A04 |
| 24 | `inspecting-tls-traffic` | `SSLKEYLOGFILE` from a Java client, and what a key log gives away | `-with-wireshark`: decrypting a local capture with tshark | A04 |
| 25 | `certificate-pinning` | Pinning a public key in a mobile-style Java client, backup pins, and the outage a pin causes when keys rotate: why browsers dropped it | – | A04, CWE-295 |

### 6. secure-coding

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 26 | `input-validation` | Allow-lists, canonicalisation, length, range and regular-expression limits (ReDoS), validated at each trust boundary | `-with-hibernate-validator`: Jakarta Validation constraints, groups and custom validators | A05, CWE-20, CWE-1333 |
| 27 | `race-conditions-and-business-logic` | A withdrawal checked and then debited, so concurrent requests spend twice; a coupon applied twice; atomic updates and idempotency keys | `-with-postgres`: row locks, isolation levels and optimistic locking with Hibernate | A06, CWE-362, CWE-840 |
| 28 | `secrets-in-errors-and-logs` | Stack traces, tokens and card numbers that leak through errors and logs; safe messages, redaction and log injection | `-with-logback`: masking patterns and structured JSON logs | A09, CWE-209, CWE-532, CWE-117 |
| 29 | `fuzzing` | Feeding a parser random and mutated input until it breaks, and turning each crash into a regression test | `-with-jazzer`: coverage-guided fuzzing of the store's parsers with Jazzer and JUnit | SSDF PW.8 |

### 7. secrets-and-supply-chain (OWASP A03, A08; SLSA)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 30 | `secrets-management` | Secrets in code, configuration, environment variables and images; run-time injection, rotation and leases | `-with-openbao`: dynamic database credentials with leases from a local OpenBao | A02, CWE-798 |
| 31 | `artifact-signing` | Signing a release and verifying it before use, with a published key and a transparency log explained | `-with-cosign`: signing a container image and refusing an unsigned one | A08, SLSA |
| 32 | `sbom-and-provenance` | A bill of materials and a provenance statement for a build, and checking them before deploying | `-with-syft-and-grype`: generating and scanning the SBOM of a local image | A03, SLSA, SSDF |
| 33 | `dependency-confusion` | A private package name that a public repository can shadow; repository order, scoping and verification | `-with-gradle`: repository content filtering and dependency verification | A03, CWE-427 |

### 8. platform-hardening

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 34 | `linux-hardening` | A Java checker for risky permissions, world-writable paths, SUID files, listening ports and default accounts, run against a prepared container | `-with-docker`: the same checks against a hardened image | CIS Benchmarks |
| 35 | `container-hardening` | What a compromised process can reach as root, with all capabilities, a writable file system and no limits | `-with-docker-and-kind`: non-root, read-only, dropped capabilities, seccomp, and Pod Security Standards on kind | CIS Docker and Kubernetes Benchmarks |

### 9. detection-and-response (OWASP A09; NIST CSF 2.0)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 36 | `security-logging` | What to log for logins, access decisions and money movements, what never to log, and tamper-evident logs | `-with-loki-and-grafana`: shipping, querying and alerting locally | A09, ASVS V16 |
| 37 | `detecting-attacks` | Detection rules over a log stream: password spraying, impossible travel, privilege changes, and measuring false positives | `-with-opensearch`: the same detections as saved queries and alerts | A09, CSF Detect |
| 38 | `incident-response` | A simulated breach timeline: prepare, detect, contain, eradicate, recover and learn, and the evidence each step needs | – | CSF Respond, NIST SP 800-61 |

### 10. secure-design (OWASP A06; SSDF)

| # | Project | Plain version | With tools | Maps to |
| ---: | --- | --- | --- | --- |
| 39 | `threat-modeling` | A data-flow model of the bank in code, with STRIDE threats found per element and trust boundary, and mitigations tracked as tests | `-with-pytm`: the same model in pytm, with generated diagrams and reports | A06, SSDF PW.1 |
| 40 | `attack-trees-and-abuse-cases` | Attack trees with costs and likelihoods, abuse cases written as failing tests, and which defence cuts the cheapest path | `-with-threat-dragon`: the store model in OWASP Threat Dragon | A06 |
| 41 | `security-requirements-with-asvs` | Choosing an ASVS level for the store and bank, turning requirements into automated checks, and reporting coverage | – | ASVS 5.0 |

## Order of work

Modules in the order above, because each builds on the one before: injection and access control first, then configuration, APIs,
identity, transport, coding, supply chain, platform, detection and design. In each module, each plain project and then its tools version.
Up to three projects are built in parallel, each in its own directory, and the indexes are updated once a batch is done.

## Checklist

| # | Project | Plain | With tools | Docs | Animation | Video | Uploaded |
| ---: | --- | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | broken-access-control | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 2 | sql-injection | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 3 | cross-site-scripting | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 4 | cross-site-request-forgery | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 5 | server-side-request-forgery | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 6 | xml-and-template-injection | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 7 | insecure-deserialization | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 8 | path-traversal-and-file-upload | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 9 | security-misconfiguration | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 10 | fail-closed-error-handling | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 11 | api-security | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 12 | session-management | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 13 | cors | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 14 | webhook-security | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 15 | jwt-done-right | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 16 | oauth2-authorization-code-pkce | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 17 | openid-connect | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 18 | tokens-in-the-browser | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 19 | passkeys | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 20 | saml-basics | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 21 | local-certificate-authority | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 22 | tls-servers-in-practice | – | ☐ | ☐ | ☐ | ☐ | ☐ |
| 23 | tls-misconfigurations | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 24 | inspecting-tls-traffic | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 25 | certificate-pinning | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 26 | input-validation | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 27 | race-conditions-and-business-logic | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 28 | secrets-in-errors-and-logs | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 29 | fuzzing | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 30 | secrets-management | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 31 | artifact-signing | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 32 | sbom-and-provenance | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 33 | dependency-confusion | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 34 | linux-hardening | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 35 | container-hardening | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 36 | security-logging | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 37 | detecting-attacks | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 38 | incident-response | ☐ | – | ☐ | ☐ | ☐ | ☐ |
| 39 | threat-modeling | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 40 | attack-trees-and-abuse-cases | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 41 | security-requirements-with-asvs | ☐ | – | ☐ | ☐ | ☐ | ☐ |

## Risks

- **Attack code in a public repository.** Every exploit targets only the project's own deliberately weak code, on loopback or in its own
  containers, and is shown next to its fix. No payload targets a real product or service.
- **Heavier labs.** Several tools projects need Docker (PostgreSQL, Redis, Keycloak, OpenBao, Loki, OpenSearch). Default tests stay
  tool-free; container tests run under `integrationTest` and always remove their containers.
- **Moving references.** OWASP lists and versions change. Each project records the edition it maps to, and pins are refreshed once a year.
