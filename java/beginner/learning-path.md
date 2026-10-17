# Learning Path: the order to follow

Work through the projects top to bottom. Each one builds on the ones before it, and a project that uses a tool or framework
comes straight after the plain version, so you see the idea first and the tool second.

Every project has the same parts: a README, runnable Java 25 code with tests, diagrams, a threat model, an animated walkthrough and a narrated video script.
Start with the project's README, run `./gradlew run`, then read its `docs/` folder.

Legend: **Plain** means written with the JDK only. **With tools** means the same idea using a library, framework or local tool.
Projects marked *planned* are not built yet.

## 1. Foundations: what security is for

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 1 | [cia-triad](foundations/cia-triad) | Plain | confidentiality, integrity and availability |
| 2 | [threats-and-risk](foundations/threats-and-risk) | Plain | assets, threats, likelihood and impact, and scoring risk |

## 2. Identity: who is asking, and what may they do

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 3 | [authentication-vs-authorization](identity/authentication-vs-authorization) | Plain | 401 and 403, sessions, roles, and stale roles |
| 4 | [authentication-vs-authorization-with-spring-security](identity/authentication-vs-authorization-with-spring-security) | With tools: Spring Security | the same ideas in a framework |

## 3. Passwords and multi-factor: proving identity

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 5 | [password-hashing](passwords-and-mfa/password-hashing) | Plain | salts, slow hashes, PBKDF2, bcrypt, scrypt, Argon2 |
| 6 | [password-hashing-with-spring-security-crypto](passwords-and-mfa/password-hashing-with-spring-security-crypto) | With tools: Spring Security crypto | delegating encoders and upgrading old hashes |
| 7 | [password-attacks-and-defences](passwords-and-mfa/password-attacks-and-defences) | Plain | guessing, stuffing, lockouts and rate limits |
| 8 | [password-attacks-and-defences-with-nginx](passwords-and-mfa/password-attacks-and-defences-with-nginx) | With tools: nginx and Docker | rate limits at the edge |
| 9 | [totp-multi-factor](passwords-and-mfa/totp-multi-factor) | Plain | one-time codes: HOTP and TOTP |
| 10 | [totp-multi-factor-with-java-otp](passwords-and-mfa/totp-multi-factor-with-java-otp) | With tools: java-otp | windows, replay and a library |

## 4. Cryptography: the building blocks

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 11 | [hashing-and-integrity](cryptography/hashing-and-integrity) | Plain | hashes, HMAC and checksums |
| 12 | [symmetric-encryption](cryptography/symmetric-encryption) | Plain | AES, ECB against GCM, and nonce reuse |
| 13 | [symmetric-encryption-with-tink](cryptography/symmetric-encryption-with-tink) | With tools: Google Tink | safe-by-default encryption and envelope encryption |
| 14 | [asymmetric-encryption](cryptography/asymmetric-encryption) | Plain | RSA-OAEP, hybrid encryption, key agreement |
| 15 | [asymmetric-encryption-with-bouncy-castle](cryptography/asymmetric-encryption-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | PEM, X.509, CMS, and interoperability |
| 16 | [digital-signatures](cryptography/digital-signatures) | Plain | Ed25519 signatures and what they prove |
| 17 | [digital-signatures-with-bouncy-castle](cryptography/digital-signatures-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | CMS, JWS, ECDSA and interoperability |
| 18 | [keystore-basics](cryptography/keystore-basics) | Plain | where keys live, PKCS12, HKDF, and key rotation |

## 5. Networking: what travels between machines

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 19 | [tcp-ip-basics](networking/tcp-ip-basics) | Plain | packets, the handshake, SYN floods, and sequence numbers |
| 20 | [tcp-ip-basics-with-tcpdump](networking/tcp-ip-basics-with-tcpdump) | With tools: tcpdump and Python | capture files and reading them |
| 21 | [dns-basics](networking/dns-basics) | Plain | how names become addresses, and how that is forged |
| 22 | [dns-basics-with-coredns](networking/dns-basics-with-coredns) | With tools: CoreDNS | a real name server in Docker |
| 23 | [http-basics-and-headers](networking/http-basics-and-headers) | Plain | requests, cookies and security headers |
| 24 | [http-basics-and-headers-with-spring-boot](networking/http-basics-and-headers-with-spring-boot) | With tools: Spring Boot | headers in a framework |
| 25 | [http-basics-and-headers-with-nginx](networking/http-basics-and-headers-with-nginx) | With tools: nginx | headers and limits at the edge |

## 6. Transport security: protecting the connection

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 26 | [tls-handshake](transport-security/tls-handshake) | Plain | how a secure channel is agreed |
| 27 | [tls-handshake-with-openssl](transport-security/tls-handshake-with-openssl) | With tools: OpenSSL | the other end of the handshake |
| 28 | [certificates-and-chain-of-trust](transport-security/certificates-and-chain-of-trust) | Plain | who vouches for a key |
| 29 | [certificates-and-chain-of-trust-with-bouncy-castle](transport-security/certificates-and-chain-of-trust-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | extensions, name constraints, CRLs |
| 30 | [https-server](transport-security/https-server) | Plain | putting it together |
| 31 | [https-server-with-spring-boot](transport-security/https-server-with-spring-boot) | With tools: Spring Boot | SSL bundles, reload and mutual TLS |
| 32 | [https-server-with-nginx](transport-security/https-server-with-nginx) | With tools: nginx | TLS termination and the forwarded-protocol trap |

## 7. Safe habits: what keeps a system safe over time

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 33 | [least-privilege](safe-habits/least-privilege) | Plain | giving only the access needed |
| 34 | [least-privilege-with-spring-security](safe-habits/least-privilege-with-spring-security) | With tools: Spring Security | method security and blast radius |
| 35 | [least-privilege-with-kubernetes-rbac](safe-habits/least-privilege-with-kubernetes-rbac) | With tools: Kubernetes RBAC on kind | roles, bindings and a linter |
| 36 | [patching-and-dependency-hygiene](safe-habits/patching-and-dependency-hygiene) | Plain | SBOMs, CVSS and patch windows |
| 37 | [patching-and-dependency-hygiene-with-trivy](safe-habits/patching-and-dependency-hygiene-with-trivy) | With tools: Trivy | scanning and dependency verification |
| 38 | [backups-and-recovery](safe-habits/backups-and-recovery) | Plain | surviving loss and ransomware |
| 39 | [backups-and-recovery-with-rustfs](safe-habits/backups-and-recovery-with-rustfs) | With tools: RustFS (S3 compatible) | versioning, object lock, write-only identity |

## Later

*Planned:* authentication-vs-authorization-with-keycloak, least-privilege-with-terraform.

## Other pages

- [README](README.md): how to run a project and build its docs and video.
- [The plan](../../docs/beginner-plan.md): the reasoning behind the order.
