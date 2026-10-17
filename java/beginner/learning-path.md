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
| 17 | [keystore-basics](cryptography/keystore-basics) | Plain | where keys live, PKCS12, HKDF, and key rotation |

## 5. Networking: what travels between machines

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 18 | [tcp-ip-basics](networking/tcp-ip-basics) | Plain | packets, the handshake, SYN floods, and sequence numbers |
| 19 | tcp-ip-basics with tcpdump and Python | With tools | *planned* |
| 20 | dns-basics | Plain | *planned*: how names become addresses, and how that is forged |
| 21 | dns-basics with CoreDNS | With tools | *planned* |
| 22 | http-basics-and-headers | Plain | *planned*: requests, cookies and security headers |
| 23 | http-basics-and-headers with Spring Boot, and with nginx | With tools | *planned* |

## 6. Transport security: protecting the connection

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 24 | tls-handshake, and with OpenSSL | Plain, then with tools | *planned*: how a secure channel is agreed |
| 25 | certificates-and-chain-of-trust, and with Bouncy Castle and an OpenSSL CA | Plain, then with tools | *planned*: who vouches for a key |
| 26 | https-server, with Spring Boot, and with nginx | Plain, then with tools | *planned*: putting it together |

## 7. Safe habits: what keeps a system safe over time

| # | Project | Kind | You learn |
| ---: | --- | --- | --- |
| 27 | least-privilege, with Spring Security, and with Docker and Kubernetes RBAC | Plain, then with tools | *planned*: giving only the access needed |
| 28 | patching-and-dependency-hygiene, and with Trivy and Dependency-Check | Plain, then with tools | *planned*: keeping software current |
| 29 | backups-and-recovery, and with MinIO and LocalStack | Plain, then with tools | *planned*: surviving loss and ransomware |

## Later

*Planned:* authentication-vs-authorization-with-keycloak, least-privilege-with-terraform, digital-signatures-with-bouncy-castle.

## Other pages

- [README](README.md): how to run a project and build its docs and video.
- [The plan](../../docs/beginner-plan.md): the reasoning behind the order.
