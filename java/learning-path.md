# Learning Path: every Java project, in order

Work through the tracks top to bottom: beginner, then intermediate, then advanced. Inside each module, a project that uses a tool or
framework comes straight after its plain version, so the idea comes first and the tool second.

Every built project has the same parts: a README, runnable Java 25 code with tests, diagrams, a threat model, an animated walkthrough and a
narrated video script. Start with the project's README, run `./gradlew run`, then read its `docs/` folder.

Legend: **Plain** means written with the JDK only. **With tools** means the same idea using a library, framework or local tool.
*Planned* projects are not built yet; *on hold* projects were started and set aside for now.

<!-- status:start -->
## Status

**Built: 105 projects. Planned: 47 projects.** Counted from the tables below by `conceptkit.sh progress`.

| Level | Module | Built | Planned | Total |
| --- | --- | ---: | ---: | ---: |
| **Beginner** | | **41** | **0** | **41** |
| | Foundations | 2 | 0 | 2 |
| | Identity | 3 | 0 | 3 |
| | Passwords and multi-factor | 6 | 0 | 6 |
| | Cryptography | 8 | 0 | 8 |
| | Networking | 7 | 0 | 7 |
| | Transport security | 7 | 0 | 7 |
| | Safe habits | 8 | 0 | 8 |
| **Intermediate** | | **64** | **12** | **76** |
| | web-security | 15 | 0 | 15 |
| | secure-configuration | 4 | 0 | 4 |
| | api-security | 7 | 0 | 7 |
| | identity-and-access | 12 | 0 | 12 |
| | tls-in-practice | 6 | 2 | 8 |
| | secure-coding | 4 | 4 | 8 |
| | secrets-and-supply-chain | 8 | 0 | 8 |
| | platform-hardening | 2 | 2 | 4 |
| | detection-and-response | 3 | 2 | 5 |
| | secure-design | 3 | 2 | 5 |
| **Advanced** | | **0** | **35** | **35** |
| | cloud-security-without-a-cloud | 0 | 3 | 3 |
| | infrastructure-as-code-security | 0 | 2 | 2 |
| | kubernetes-and-container-security | 0 | 4 | 4 |
| | devsecops-pipelines | 0 | 4 | 4 |
| | service-identity-and-zero-trust | 0 | 4 | 4 |
| | applied-cryptography-and-pki | 0 | 5 | 5 |
| | detection-engineering | 0 | 3 | 3 |
| | offensive-security-methodology | 0 | 4 | 4 |
| | resilience-and-recovery | 0 | 2 | 2 |
| | governance-risk-and-compliance | 0 | 4 | 4 |
| **All** | | **105** | **47** | **152** |

**By kind.** *Plain* uses the JDK only; *With tools* uses a library, framework or local tool; *Unclassified* are advanced projects not yet designed as one or the other.

| Level | Kind | Built | Planned | Total |
| --- | --- | ---: | ---: | ---: |
| Beginner | Plain | 20 | 0 | 20 |
| Beginner | With tools | 21 | 0 | 21 |
| Intermediate | Plain | 40 | 0 | 40 |
| Intermediate | With tools | 24 | 12 | 36 |
| Advanced | Unclassified | 0 | 35 | 35 |
| **All** | **Plain** | **60** | **0** | **60** |
| **All** | **With tools** | **45** | **12** | **57** |
| **All** | **Unclassified** | **0** | **35** | **35** |
<!-- status:end -->

## Beginner: foundations and core concepts

Track index: [beginner](beginner/README.md). Plan: [beginner plan](../docs/beginner-plan.md).

### 1. Foundations: what security is for

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 1 | [cia-triad](beginner/foundations/cia-triad) | Plain | confidentiality, integrity and availability |
| 2 | [threats-and-risk](beginner/foundations/threats-and-risk) | Plain | assets, threats, likelihood and impact, and scoring risk |

### 2. Identity: who is asking, and what may they do

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 3 | [authentication-vs-authorization](beginner/identity/authentication-vs-authorization) | Plain | 401 and 403, sessions, roles, and stale roles |
| 4 | [authentication-vs-authorization-with-spring-security](beginner/identity/authentication-vs-authorization-with-spring-security) | With tools: Spring Security | the same ideas in a framework |
| 5 | [authentication-vs-authorization-with-keycloak](beginner/identity/authentication-vs-authorization-with-keycloak) | With tools: Keycloak | tokens from a real login server |

### 3. Passwords and multi-factor: proving identity

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 6 | [password-hashing](beginner/passwords-and-mfa/password-hashing) | Plain | salts, slow hashes, PBKDF2, bcrypt, scrypt, Argon2 |
| 7 | [password-hashing-with-spring-security-crypto](beginner/passwords-and-mfa/password-hashing-with-spring-security-crypto) | With tools: Spring Security crypto | delegating encoders and upgrading old hashes |
| 8 | [password-attacks-and-defences](beginner/passwords-and-mfa/password-attacks-and-defences) | Plain | guessing, stuffing, lockouts and rate limits |
| 9 | [password-attacks-and-defences-with-nginx](beginner/passwords-and-mfa/password-attacks-and-defences-with-nginx) | With tools: nginx and Docker | rate limits at the edge |
| 10 | [totp-multi-factor](beginner/passwords-and-mfa/totp-multi-factor) | Plain | one-time codes: HOTP and TOTP |
| 11 | [totp-multi-factor-with-java-otp](beginner/passwords-and-mfa/totp-multi-factor-with-java-otp) | With tools: java-otp | windows, replay and a library |

### 4. Cryptography: the building blocks

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 12 | [hashing-and-integrity](beginner/cryptography/hashing-and-integrity) | Plain | hashes, HMAC and checksums |
| 13 | [symmetric-encryption](beginner/cryptography/symmetric-encryption) | Plain | AES, ECB against GCM, and nonce reuse |
| 14 | [symmetric-encryption-with-tink](beginner/cryptography/symmetric-encryption-with-tink) | With tools: Google Tink | safe-by-default encryption and envelope encryption |
| 15 | [asymmetric-encryption](beginner/cryptography/asymmetric-encryption) | Plain | RSA-OAEP, hybrid encryption, key agreement |
| 16 | [asymmetric-encryption-with-bouncy-castle](beginner/cryptography/asymmetric-encryption-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | PEM, X.509, CMS, and interoperability |
| 17 | [digital-signatures](beginner/cryptography/digital-signatures) | Plain | Ed25519 signatures and what they prove |
| 18 | [digital-signatures-with-bouncy-castle](beginner/cryptography/digital-signatures-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | CMS, JWS, ECDSA and interoperability |
| 19 | [keystore-basics](beginner/cryptography/keystore-basics) | Plain | where keys live, PKCS12, HKDF, and key rotation |

### 5. Networking: what travels between machines

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 20 | [tcp-ip-basics](beginner/networking/tcp-ip-basics) | Plain | packets, the handshake, SYN floods, and sequence numbers |
| 21 | [tcp-ip-basics-with-tcpdump](beginner/networking/tcp-ip-basics-with-tcpdump) | With tools: tcpdump and Python | capture files and reading them |
| 22 | [dns-basics](beginner/networking/dns-basics) | Plain | how names become addresses, and how that is forged |
| 23 | [dns-basics-with-coredns](beginner/networking/dns-basics-with-coredns) | With tools: CoreDNS | a real name server in Docker |
| 24 | [http-basics-and-headers](beginner/networking/http-basics-and-headers) | Plain | requests, cookies and security headers |
| 25 | [http-basics-and-headers-with-spring-boot](beginner/networking/http-basics-and-headers-with-spring-boot) | With tools: Spring Boot | headers in a framework |
| 26 | [http-basics-and-headers-with-nginx](beginner/networking/http-basics-and-headers-with-nginx) | With tools: nginx | headers and limits at the edge |

### 6. Transport security: protecting the connection

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 27 | [tls-handshake](beginner/transport-security/tls-handshake) | Plain | how a secure channel is agreed |
| 28 | [tls-handshake-with-openssl](beginner/transport-security/tls-handshake-with-openssl) | With tools: OpenSSL | the other end of the handshake |
| 29 | [certificates-and-chain-of-trust](beginner/transport-security/certificates-and-chain-of-trust) | Plain | who vouches for a key |
| 30 | [certificates-and-chain-of-trust-with-bouncy-castle](beginner/transport-security/certificates-and-chain-of-trust-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | extensions, name constraints, CRLs |
| 31 | [https-server](beginner/transport-security/https-server) | Plain | putting it together |
| 32 | [https-server-with-spring-boot](beginner/transport-security/https-server-with-spring-boot) | With tools: Spring Boot | SSL bundles, reload and mutual TLS |
| 33 | [https-server-with-nginx](beginner/transport-security/https-server-with-nginx) | With tools: nginx | TLS termination and the forwarded-protocol trap |

### 7. Safe habits: what keeps a system safe over time

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 34 | [least-privilege](beginner/safe-habits/least-privilege) | Plain | giving only the access needed |
| 35 | [least-privilege-with-spring-security](beginner/safe-habits/least-privilege-with-spring-security) | With tools: Spring Security | method security and blast radius |
| 36 | [least-privilege-with-kubernetes-rbac](beginner/safe-habits/least-privilege-with-kubernetes-rbac) | With tools: Kubernetes RBAC on kind | roles, bindings and a linter |
| 37 | [patching-and-dependency-hygiene](beginner/safe-habits/patching-and-dependency-hygiene) | Plain | SBOMs, CVSS and patch windows |
| 38 | [patching-and-dependency-hygiene-with-trivy](beginner/safe-habits/patching-and-dependency-hygiene-with-trivy) | With tools: Trivy | scanning and dependency verification |
| 39 | [backups-and-recovery](beginner/safe-habits/backups-and-recovery) | Plain | surviving loss and ransomware |
| 40 | [backups-and-recovery-with-rustfs](beginner/safe-habits/backups-and-recovery-with-rustfs) | With tools: RustFS (S3 compatible) | versioning, object lock, write-only identity |
| 41 | [least-privilege-with-terraform](beginner/safe-habits/least-privilege-with-terraform) | With tools: OpenTofu and LocalStack | IAM policies as code |

## Intermediate: applied security

Track index: [intermediate](intermediate/README.md). Plan, with the OWASP, ASVS, CWE and RFC references each project maps to: [intermediate plan](../docs/intermediate-plan.md).

### 1. web-security: injection and access control (OWASP A01, A05)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 42 | [broken-access-control](intermediate/web-security/broken-access-control) | Plain | Signed in is not the same as allowed | A01, API1, CWE-639, CWE-862 |
| 43 | [broken-access-control-with-spring-security](intermediate/web-security/broken-access-control-with-spring-security) | With tools: spring security | A framework enforces exactly the rules it is given | A01, API1, CWE-639, CWE-862 |
| 44 | [sql-injection](intermediate/web-security/sql-injection) | Plain | Text typed by a customer must never become code | A05, CWE-89 |
| 45 | [sql-injection-with-hibernate-and-postgres](intermediate/web-security/sql-injection-with-hibernate-and-postgres) | With tools: hibernate and postgres | Hibernate does not make a query safe: only parameters do, and a database user with few rights limits the damage of a mistake | A05, CWE-89 |
| 46 | [cross-site-scripting](intermediate/web-security/cross-site-scripting) | Plain | Text from a customer must stay text | A05, CWE-79 |
| 47 | [cross-site-scripting-with-spring-boot](intermediate/web-security/cross-site-scripting-with-spring-boot) | With tools: spring boot | Let the template escape, and know the one switch that stops it | A05, CWE-79 |
| 48 | [cross-site-request-forgery](intermediate/web-security/cross-site-request-forgery) | Plain | A cookie proves who is asking, not what they meant | A01, CWE-352 |
| 49 | [cross-site-request-forgery-with-spring-security](intermediate/web-security/cross-site-request-forgery-with-spring-security) | With tools: spring security | Spring Security's token check is on by default, and the ways to lose it are a switch, an exemption and a GET | A01, CWE-352 |
| 50 | [server-side-request-forgery](intermediate/web-security/server-side-request-forgery) | Plain | When the server fetches an address for a customer, it fetches it from inside | A01, CWE-918 |
| 51 | [server-side-request-forgery-with-spring-boot](intermediate/web-security/server-side-request-forgery-with-spring-boot) | With tools: spring boot | A Spring RestClient fetches whatever it is told, so the address must be judged after resolution and the connection must go to the address that was judged | A01, CWE-918 |
| 52 | [xml-and-template-injection](intermediate/web-security/xml-and-template-injection) | Plain | A file or a template is a program for its reader | A05, CWE-611, CWE-1336 |
| 53 | [insecure-deserialization](intermediate/web-security/insecure-deserialization) | Plain | Rebuilding an object from bytes runs code the sender chose | A08, CWE-502 |
| 54 | [insecure-deserialization-with-jackson](intermediate/web-security/insecure-deserialization-with-jackson) | With tools: jackson | JSON is data until a type id tells Jackson which class to build, and then the cookie is choosing code | A08, CWE-502 |
| 55 | [path-traversal-and-file-upload](intermediate/web-security/path-traversal-and-file-upload) | Plain | A file name from a customer is a path until proven otherwise | A01, CWE-22, CWE-434, CWE-601 |
| 56 | [path-traversal-and-file-upload-with-spring-boot](intermediate/web-security/path-traversal-and-file-upload-with-spring-boot) | With tools: spring boot | Spring hands you the file name exactly as the customer sent it, and its static handler follows a link out of the folder | A01, CWE-22, CWE-434, CWE-601 |

### 2. secure-configuration: misconfiguration and exceptional conditions (OWASP A02, A10)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 57 | [security-misconfiguration](intermediate/secure-configuration/security-misconfiguration) | Plain | The settings nobody changed are the first thing an attacker checks | A02, CWE-16 |
| 58 | [security-misconfiguration-with-spring-boot-actuator](intermediate/secure-configuration/security-misconfiguration-with-spring-boot-actuator) | With tools: spring boot actuator | One line that exposes every actuator endpoint hands a stranger the environment, the route map and the memory of the store | A02, CWE-16 |
| 59 | [fail-closed-error-handling](intermediate/secure-configuration/fail-closed-error-handling) | Plain | When something breaks, the answer must be no | A10, CWE-636, CWE-755 |
| 60 | [fail-closed-error-handling-with-spring-boot](intermediate/secure-configuration/fail-closed-error-handling-with-spring-boot) | With tools: spring boot | A catch-all that returns yes, a transaction that does not roll back and an error page that names the server are all one Spring Boot default away | A10, CWE-636, CWE-755 |

### 3. api-security: APIs and sessions (OWASP API Top 10)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 61 | [api-security](intermediate/api-security/api-security) | Plain | An API owes its callers three limits: what they may set, what they may see, and how much they may ask for | API3, API4, API6 |
| 62 | [api-security-with-spring-boot-and-redis](intermediate/api-security/api-security-with-spring-boot-and-redis) | With tools: spring boot and redis | Bind a body to the stored record and a caller sets their own role; return the record and they read everyone's; count nothing and they never stop | API3, API4, API6 |
| 63 | [session-management](intermediate/api-security/session-management) | Plain | A session id is a password that the server hands out | A07, CWE-384 |
| 64 | [session-management-with-spring-session-and-redis](intermediate/api-security/session-management-with-spring-session-and-redis) | With tools: spring session and redis | Moving sessions into Redis makes them survive a restart and a second server, and makes Redis the place where a stolen key is a live session | A07, CWE-384 |
| 65 | [cors](intermediate/api-security/cors) | Plain | CORS decides who may read an answer, not who may send a request | A02, CWE-942 |
| 66 | [cors-with-spring-boot](intermediate/api-security/cors-with-spring-boot) | With tools: spring boot | Spring's CORS answers 403 before your code runs, refuses a wildcard with credentials, and still reflects every origin if you ask it to | A02, CWE-942 |
| 67 | [webhook-security](intermediate/api-security/webhook-security) | Plain | A message to a public address proves nothing until it is verified | API8, API10, CWE-345 |

### 4. identity-and-access: modern authentication and authorization (OWASP A07, RFC 9700)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 68 | [jwt-done-right](intermediate/identity-and-access/jwt-done-right) | Plain | Decoding a token is not verifying it | A07, CWE-347 |
| 69 | [jwt-done-right-with-nimbus-jose-jwt](intermediate/identity-and-access/jwt-done-right-with-nimbus-jose-jwt) | With tools: nimbus jose jwt | A standard library does the parsing, the signature and most claims; the algorithm, the type, the key set URL and the policy are still the API's to set | A07, CWE-347 |
| 70 | [oauth2-authorization-code-pkce](intermediate/identity-and-access/oauth2-authorization-code-pkce) | Plain | Give an app access without giving it your password | RFC 9700, A07 |
| 71 | [oauth2-authorization-code-pkce-with-keycloak](intermediate/identity-and-access/oauth2-authorization-code-pkce-with-keycloak) | With tools: keycloak | The same flow against a real authorization server: the checks that make it safe are settings on the client, and Keycloak enforces them | RFC 9700, A07 |
| 72 | [openid-connect](intermediate/identity-and-access/openid-connect) | Plain | Access is not identity | A07 |
| 73 | [openid-connect-with-spring-security-and-keycloak](intermediate/identity-and-access/openid-connect-with-spring-security-and-keycloak) | With tools: spring security and keycloak | Spring Security checks the ID token and keeps the state and nonce; what it leaves to the store is who the customer is, and how sign-out reaches every session | A07 |
| 74 | [tokens-in-the-browser](intermediate/identity-and-access/tokens-in-the-browser) | Plain | A token a script can read is a token an attacker can take | OAuth for browser-based apps, RFC 9449 |
| 75 | [tokens-in-the-browser-with-spring-boot](intermediate/identity-and-access/tokens-in-the-browser-with-spring-boot) | With tools: spring boot | Keep the tokens on the server with a Spring Boot backend for the frontend, and bind any token the browser must hold to a key | OAuth for browser-based apps, RFC 9449 |
| 76 | [passkeys](intermediate/identity-and-access/passkeys) | Plain | A key that only signs for the real site | A07, NIST SP 800-63B |
| 77 | [passkeys-with-spring-security](intermediate/identity-and-access/passkeys-with-spring-security) | With tools: spring security | Spring Security 7 gives a bank passkey registration and sign-in in a few lines; user verification, clone detection and the password you keep are still the bank's to decide | A07, NIST SP 800-63B |
| 78 | [saml-basics](intermediate/identity-and-access/saml-basics) | Plain | A valid signature is not the same as the statement you believed | A07, CWE-347 |
| 79 | [saml-basics-with-keycloak](intermediate/identity-and-access/saml-basics-with-keycloak) | With tools: keycloak | A SAML service provider against a real identity provider: the library refuses every signature that is not the bank's, and a key rotation at the bank locks the shop out | A07, CWE-347 |

### 5. tls-in-practice

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 80 | [local-certificate-authority](intermediate/tls-in-practice/local-certificate-authority) | Plain | A certificate is a signed claim about a name, and revocation is only as good as the list the client has | A04 |
| 81 | [local-certificate-authority-with-step-ca](intermediate/tls-in-practice/local-certificate-authority-with-step-ca) | With tools: step ca | Run a real certificate authority for the store's own services: tokens and ACME, a policy for names, day-long certificates that renew, and a revocation list that only helps clients that read it | A04 |
| 82 | [tls-servers-in-practice-with-nginx-and-caddy](intermediate/tls-in-practice/tls-servers-in-practice-with-nginx-and-caddy) | With tools: nginx and caddy | The same shop behind nginx, where every TLS choice is a line you write, and behind Caddy, which makes its own certificates and renews them | A02, A04 |
| 83 | [tls-misconfigurations](intermediate/tls-in-practice/tls-misconfigurations) | Plain | A server's TLS is a set of choices, and a checker can read all of them | A02, A04 |
| 84 | tls-misconfigurations-with-testssl | With tools: testssl | *planned*: testssl.sh against deliberately misconfigured local servers | A02, A04 |
| 85 | [inspecting-tls-traffic](intermediate/tls-in-practice/inspecting-tls-traffic) | Plain | A key log opens exactly the connections it lists, and a leaked server key opens nothing that was ephemeral | A04 |
| 86 | inspecting-tls-traffic-with-wireshark | With tools: wireshark | *planned*: decrypting a local capture with tshark | A04 |
| 87 | [certificate-pinning](intermediate/tls-in-practice/certificate-pinning) | Plain | A pin trusts one key instead of every authority, and the price is paid on the day the key changes | A04, CWE-295 |

### 6. secure-coding

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 88 | [input-validation](intermediate/secure-coding/input-validation) | Plain | Decide what is allowed, at the boundary, before it costs anything | A05, CWE-20, CWE-1333 |
| 89 | input-validation-with-hibernate-validator | With tools: hibernate validator | *planned*: Jakarta Validation constraints, groups and custom validators | A05, CWE-20, CWE-1333 |
| 90 | [race-conditions-and-business-logic](intermediate/secure-coding/race-conditions-and-business-logic) | Plain | Correct one at a time is not correct at the same time | A06, CWE-362, CWE-840 |
| 91 | race-conditions-and-business-logic-with-postgres | With tools: postgres | *planned*: row locks, isolation levels and optimistic locking with Hibernate | A06, CWE-362, CWE-840 |
| 92 | [secrets-in-errors-and-logs](intermediate/secure-coding/secrets-in-errors-and-logs) | Plain | A log is a second database that nobody protects | A09, CWE-209, CWE-532, CWE-117 |
| 93 | secrets-in-errors-and-logs-with-logback | With tools: logback | *planned*: masking patterns and structured JSON logs | A09, CWE-209, CWE-532, CWE-117 |
| 94 | [fuzzing](intermediate/secure-coding/fuzzing) | Plain | Let the machine try the inputs nobody thought of | SSDF PW.8 |
| 95 | fuzzing-with-jazzer | With tools: jazzer | *planned*: coverage-guided fuzzing of the store's parsers with Jazzer and JUnit | SSDF PW.8 |

### 7. secrets-and-supply-chain (OWASP A03, A08; SLSA)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 96 | [secrets-management](intermediate/secrets-and-supply-chain/secrets-management) | Plain | A secret that never sits still is a secret that leaks less | A02, CWE-798 |
| 97 | [secrets-management-with-openbao](intermediate/secrets-and-supply-chain/secrets-management-with-openbao) | With tools: openbao | A real secrets server hands each application its own short-lived database login, and takes it back when the lease ends or a leak is noticed | A02, CWE-798 |
| 98 | [artifact-signing](intermediate/secrets-and-supply-chain/artifact-signing) | Plain | Run only what the release team signed, and only what the world can see they signed | A08, SLSA |
| 99 | [artifact-signing-with-cosign](intermediate/secrets-and-supply-chain/artifact-signing-with-cosign) | With tools: cosign | A tag is a name, a digest is a checksum, and a signature on the digest is the only thing a deployment gate can rely on | A08, SLSA |
| 100 | [sbom-and-provenance](intermediate/secrets-and-supply-chain/sbom-and-provenance) | Plain | Know what is inside a release, and prove where it came from | A03, SLSA, SSDF |
| 101 | [sbom-and-provenance-with-syft-and-grype](intermediate/secrets-and-supply-chain/sbom-and-provenance-with-syft-and-grype) | With tools: syft and grype | A bill of materials says what is inside an image, a scanner says which of it is known to be broken, and a gate decides what to do about it | A03, SLSA, SSDF |
| 102 | [dependency-confusion](intermediate/secrets-and-supply-chain/dependency-confusion) | Plain | If a build can find your private name in a public place, someone else will put a package there | A03, CWE-427 |
| 103 | [dependency-confusion-with-gradle](intermediate/secrets-and-supply-chain/dependency-confusion-with-gradle) | With tools: gradle | Gradle asks every repository for every name unless it is told which repository may answer for which group, and then it checks the bytes against a list | A03, CWE-427 |

### 8. platform-hardening

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 104 | [linux-hardening](intermediate/platform-hardening/linux-hardening) | Plain | Most host compromises begin with a setting that nobody looked at | CIS Benchmarks |
| 105 | linux-hardening-with-docker | With tools: docker | *planned*: the same checks against a hardened image | CIS Benchmarks |
| 106 | [container-hardening](intermediate/platform-hardening/container-hardening) | Plain | Assume the process is taken over, and ask what it can reach | CIS Docker and Kubernetes Benchmarks |
| 107 | container-hardening-with-docker-and-kind | With tools: docker and kind | *planned*: non-root, read-only, dropped capabilities, seccomp, and Pod Security Standards on kind | CIS Docker and Kubernetes Benchmarks |

### 9. detection-and-response (OWASP A09; NIST CSF 2.0)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 108 | [security-logging](intermediate/detection-and-response/security-logging) | Plain | A log is only useful if it can answer who did it, and only safe if it holds no secrets | A09, ASVS V16 |
| 109 | security-logging-with-loki-and-grafana | With tools: loki and grafana | *planned*: shipping, querying and alerting locally | A09, ASVS V16 |
| 110 | [detecting-attacks](intermediate/detection-and-response/detecting-attacks) | Plain | A detection rule is a hypothesis about an attacker, and it must be measured against a normal day | A09, CSF Detect |
| 111 | detecting-attacks-with-opensearch | With tools: opensearch | *planned*: the same detections as saved queries and alerts | A09, CSF Detect |
| 112 | [incident-response](intermediate/detection-and-response/incident-response) | Plain | The outcome of an incident is mostly decided before it starts, and by the order of the first hour | CSF Respond, NIST SP 800-61 |

### 10. secure-design (OWASP A06; SSDF)

| # | Project | Kind | You learn | Maps to |
| ---: | --- | --- | --- | --- |
| 113 | [threat-modeling](intermediate/secure-design/threat-modeling) | Plain | Find the threats on paper, before an attacker finds them in production | A06, SSDF PW.1 |
| 114 | threat-modeling-with-pytm | With tools: pytm | *planned*: the same model in pytm, with generated diagrams and reports | A06, SSDF PW.1 |
| 115 | [attack-trees-and-abuse-cases](intermediate/secure-design/attack-trees-and-abuse-cases) | Plain | Draw how the attacker wins, price each step, then cut the cheapest path and look again | A06 |
| 116 | attack-trees-and-abuse-cases-with-threat-dragon | With tools: threat dragon | *planned*: the store model in OWASP Threat Dragon | A06 |
| 117 | [security-requirements-with-asvs](intermediate/secure-design/security-requirements-with-asvs) | Plain | Turn a verification standard into requirements that check themselves | ASVS 5.0 |

## Advanced: specialized topics

All *planned*. Plan: [advanced plan](../docs/advanced-plan.md).

### 1. cloud-security-without-a-cloud

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 118 | cloud-iam-models | *planned*: Identity, resource and session policies, role assumption and confused-deputy attacks, evaluated in Java | LocalStack, OpenTofu |
| 119 | network-isolation | *planned*: Segmentation, security groups as code, and what a flat network lets an attacker reach | Docker networks, kind with network policies |
| 120 | cloud-misconfigurations | *planned*: Public buckets, wildcard policies, metadata endpoints and unencrypted storage, found before deploy | LocalStack, Checkov, Trivy |

### 2. infrastructure-as-code-security

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 121 | iac-scanning | *planned*: Writing a rule, then running scanners on OpenTofu and Kubernetes manifests, and handling false positives and exceptions | Checkov, Trivy, KICS |
| 122 | policy-as-code | *planned*: Deny-by-default admission and deployment rules written as policy, with tests | OPA and Rego, Conftest |

### 3. kubernetes-and-container-security

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 123 | kubernetes-admission-control | *planned*: Blocking privileged pods, unsigned images and missing limits at admission | kind, Kyverno |
| 124 | kubernetes-network-policies | *planned*: Default-deny traffic between namespaces and pods, and testing it | kind, Calico or Cilium |
| 125 | runtime-security | *planned*: Detecting a shell in a container, unexpected writes and outbound connections | kind, Falco |
| 126 | kubernetes-secrets | *planned*: Why base64 is not encryption; encryption at rest, external secrets and short-lived tokens | kind, OpenBao |

### 4. devsecops-pipelines

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 127 | sast | *planned*: Static analysis: how taint tracking finds injection, writing a custom rule, triage | Semgrep, CodeQL CLI |
| 128 | dast | *planned*: Scanning the running intermediate store, authenticated scans, and reading a report | OWASP ZAP |
| 129 | secret-scanning | *planned*: Finding secrets in history and blocking them before push | gitleaks, pre-commit hooks |
| 130 | secure-pipeline | *planned*: A local pipeline with signed, reproducible builds, provenance and gates that fail closed | Gradle, cosign, SLSA provenance, a local Gitea and Woodpecker CI |

### 5. service-identity-and-zero-trust

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 131 | mutual-tls-between-services | *planned*: Client certificates for service-to-service calls, authorization by identity, and rotation | Spring Boot, step-ca |
| 132 | service-mesh-mtls | *planned*: Mesh-issued identities and automatic mTLS, and what the mesh does not protect | kind, Linkerd |
| 133 | workload-identity | *planned*: SPIFFE IDs and short-lived SVIDs instead of shared secrets | SPIRE |
| 134 | zero-trust-access | *planned*: Every request authenticated, authorized and logged, close to the resource; an identity-aware proxy | Keycloak, OPA, Envoy or oauth2-proxy |

### 6. applied-cryptography-and-pki

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 135 | certificate-lifecycle | *planned*: Issuance, OCSP and CRLs, stapling, short-lived certificates and automated rotation, and the outages that missed renewals cause | step-ca, OpenSSL |
| 136 | tls-attacks-and-downgrades | *planned*: Downgrades, weak key exchange, and why TLS 1.3 removed what it removed, shown on deliberately weak local servers | OpenSSL, testssl.sh |
| 137 | post-quantum-cryptography | *planned*: ML-KEM and ML-DSA in the JDK, hybrid key exchange, and planning a migration | JDK 25, Bouncy Castle |
| 138 | crypto-agility | *planned*: Inventorying algorithms, versioned formats and rotating an algorithm without losing data | JDK 25, Tink |
| 139 | hardware-backed-keys | *planned*: Keys that never leave a token; signing with PKCS#11 | SoftHSM, the JDK PKCS#11 provider |

### 7. detection-engineering

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 140 | detection-as-code | *planned*: Sigma rules mapped to ATT&CK, tested against recorded logs, with false-positive budgets | Sigma, pySigma, OpenSearch |
| 141 | siem-basics | *planned*: Collecting, normalizing and correlating events from the store, the database and the hosts | Wazuh or OpenSearch Security Analytics |
| 142 | threat-hunting | *planned*: Hypothesis-driven hunts over a week of simulated activity | OpenSearch, Jupyter |

### 8. offensive-security-methodology

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 143 | penetration-testing-methodology | *planned*: Scoping, rules of engagement, reconnaissance, testing, reporting and retesting, against the project's own vulnerable store | OWASP ZAP, the intermediate store |
| 144 | vulnerability-research | *planned*: From a crash found by fuzzing to a root cause, a patch and an advisory, with coordinated disclosure | Jazzer, a debugger |
| 145 | reverse-engineering-fundamentals | *planned*: Reading Java bytecode and a decompiled jar, finding a hard-coded key, and obfuscation's limits | javap, CFR, Ghidra |
| 146 | malware-analysis-fundamentals | *planned*: Static triage of a harmless, purpose-built sample: strings, hashes, YARA rules and sandbox safety | YARA, a disposable container |

### 9. resilience-and-recovery

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 147 | ransomware-resilience | *planned*: Immutable backups, isolated recovery, and a timed restore exercise | RustFS object lock, OpenTofu |
| 148 | security-chaos-engineering | *planned*: Injecting failures into security controls and checking that they fail closed and alert | Toxiproxy, the intermediate store |

### 10. governance-risk-and-compliance

| # | Project | You learn | Tools |
| ---: | --- | --- | --- |
| 149 | risk-management | *planned*: A risk register with quantified risk (FAIR-style ranges), treatment decisions and residual risk | Java, Monte Carlo simulation |
| 150 | controls-and-evidence | *planned*: Mapping controls to NIST CSF 2.0 and ISO 27001, and collecting evidence automatically from tests | Java, the earlier projects' tests |
| 151 | security-architecture-review | *planned*: Reviewing a design: trust boundaries, data flows, and decision records | Structurizr (C4), ADRs |
| 152 | privacy-engineering | *planned*: Data minimization, pseudonymization, retention and subject access in a bank's data model | Java, PostgreSQL |

## Other pages

- [Beginner learning path](beginner/learning-path.md): the beginner track on its own.
- [Repository README](../README.md): the scope of the whole repository.
