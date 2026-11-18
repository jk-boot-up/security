# Learning Path: every Java project, in order

Work through the tracks top to bottom: beginner, then intermediate, then advanced. Inside each module, a project that uses a tool or
framework comes straight after its plain version, so the idea comes first and the tool second.

Every built project has the same parts: a README, runnable Java 25 code with tests, diagrams, a threat model, an animated walkthrough and a
narrated video script. Start with the project's README, run `./gradlew run`, then read its `docs/` folder.

Legend: **Plain** means written with the JDK only. **With tools** means the same idea using a library, framework or local tool.
*Planned* projects are not built yet; *on hold* projects were started and set aside for now.

<!-- status:start -->
## Status

**Built: 151 projects. Planned: 0 projects.** Counted from the tables below by `conceptkit.sh progress`.

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
| **Intermediate** | | **76** | **0** | **76** |
| | web-security | 15 | 0 | 15 |
| | secure-configuration | 4 | 0 | 4 |
| | api-security | 7 | 0 | 7 |
| | identity-and-access | 12 | 0 | 12 |
| | tls-in-practice | 8 | 0 | 8 |
| | secure-coding | 8 | 0 | 8 |
| | secrets-and-supply-chain | 8 | 0 | 8 |
| | platform-hardening | 4 | 0 | 4 |
| | detection-and-response | 5 | 0 | 5 |
| | secure-design | 5 | 0 | 5 |
| **Advanced** | | **34** | **0** | **34** |
| | cloud-security-without-a-cloud | 3 | 0 | 3 |
| | infrastructure-as-code-security | 2 | 0 | 2 |
| | kubernetes-and-container-security | 4 | 0 | 4 |
| | devsecops-pipelines | 4 | 0 | 4 |
| | service-identity-and-zero-trust | 4 | 0 | 4 |
| | applied-cryptography-and-pki | 5 | 0 | 5 |
| | detection-engineering | 3 | 0 | 3 |
| | offensive-security-methodology | 3 | 0 | 3 |
| | resilience-and-recovery | 2 | 0 | 2 |
| | governance-risk-and-compliance | 4 | 0 | 4 |
| **All** | | **151** | **0** | **151** |

**By kind.** *Plain* uses the JDK only; *With tools* uses a library, framework or local tool; *Unclassified* are advanced projects not yet designed as one or the other.

| Level | Kind | Built | Planned | Total |
| --- | --- | ---: | ---: | ---: |
| Beginner | Plain | 20 | 0 | 20 |
| Beginner | With tools | 21 | 0 | 21 |
| Intermediate | Plain | 40 | 0 | 40 |
| Intermediate | With tools | 36 | 0 | 36 |
| Advanced | Plain | 5 | 0 | 5 |
| Advanced | With tools | 29 | 0 | 29 |
| **All** | **Plain** | **65** | **0** | **65** |
| **All** | **With tools** | **86** | **0** | **86** |
<!-- status:end -->

## Beginner: foundations and core concepts

Track index: [beginner](beginner/README.md). Plan: [beginner plan](../docs/beginner-plan.md).

### 1. Foundations: what security is for

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 1 | [cia-triad](beginner/foundations/cia-triad) | Plain | confidentiality, integrity and availability | [Watch](https://youtu.be/da4jfQA4wcM) |
| 2 | [threats-and-risk](beginner/foundations/threats-and-risk) | Plain | assets, threats, likelihood and impact, and scoring risk | [Watch](https://youtu.be/mFAS4-1wL2k) |

### 2. Identity: who is asking, and what may they do

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 3 | [authentication-vs-authorization](beginner/identity/authentication-vs-authorization) | Plain | 401 and 403, sessions, roles, and stale roles | [Watch](https://youtu.be/5G1Ya9y2JD4) |
| 4 | [authentication-vs-authorization-with-spring-security](beginner/identity/authentication-vs-authorization-with-spring-security) | With tools: Spring Security | the same ideas in a framework | [Watch](https://youtu.be/McxfX_ZUIJQ) |
| 5 | [authentication-vs-authorization-with-keycloak](beginner/identity/authentication-vs-authorization-with-keycloak) | With tools: Keycloak | tokens from a real login server | [Watch](https://youtu.be/Zhg9BkPCTHU) |

### 3. Passwords and multi-factor: proving identity

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 6 | [password-hashing](beginner/passwords-and-mfa/password-hashing) | Plain | salts, slow hashes, PBKDF2, bcrypt, scrypt, Argon2 | [Watch](https://youtu.be/54dqhc42gvI) |
| 7 | [password-hashing-with-spring-security-crypto](beginner/passwords-and-mfa/password-hashing-with-spring-security-crypto) | With tools: Spring Security crypto | delegating encoders and upgrading old hashes | [Watch](https://youtu.be/ancrHfCWTlk) |
| 8 | [password-attacks-and-defences](beginner/passwords-and-mfa/password-attacks-and-defences) | Plain | guessing, stuffing, lockouts and rate limits | [Watch](https://youtu.be/zM4K_SYnn9c) |
| 9 | [password-attacks-and-defences-with-nginx](beginner/passwords-and-mfa/password-attacks-and-defences-with-nginx) | With tools: nginx and Docker | rate limits at the edge | [Watch](https://youtu.be/ADF6Buz8wUo) |
| 10 | [totp-multi-factor](beginner/passwords-and-mfa/totp-multi-factor) | Plain | one-time codes: HOTP and TOTP | [Watch](https://youtu.be/2TMzaj2utG4) |
| 11 | [totp-multi-factor-with-java-otp](beginner/passwords-and-mfa/totp-multi-factor-with-java-otp) | With tools: java-otp | windows, replay and a library | [Watch](https://youtu.be/lDTd6UUR8PM) |

### 4. Cryptography: the building blocks

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 12 | [hashing-and-integrity](beginner/cryptography/hashing-and-integrity) | Plain | hashes, HMAC and checksums | [Watch](https://youtu.be/O2CuHz11LlA) |
| 13 | [symmetric-encryption](beginner/cryptography/symmetric-encryption) | Plain | AES, ECB against GCM, and nonce reuse | [Watch](https://youtu.be/mIlukJjoIP4) |
| 14 | [symmetric-encryption-with-tink](beginner/cryptography/symmetric-encryption-with-tink) | With tools: Google Tink | safe-by-default encryption and envelope encryption | [Watch](https://youtu.be/11lfP6wewq4) |
| 15 | [asymmetric-encryption](beginner/cryptography/asymmetric-encryption) | Plain | RSA-OAEP, hybrid encryption, key agreement | [Watch](https://youtu.be/dHN8KUq9SeU) |
| 16 | [asymmetric-encryption-with-bouncy-castle](beginner/cryptography/asymmetric-encryption-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | PEM, X.509, CMS, and interoperability | [Watch](https://youtu.be/DgHTgfhIe-U) |
| 17 | [digital-signatures](beginner/cryptography/digital-signatures) | Plain | Ed25519 signatures and what they prove | [Watch](https://youtu.be/yL0tehqx_hU) |
| 18 | [digital-signatures-with-bouncy-castle](beginner/cryptography/digital-signatures-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | CMS, JWS, ECDSA and interoperability | [Watch](https://youtu.be/qb8h_nTWbk0) |
| 19 | [keystore-basics](beginner/cryptography/keystore-basics) | Plain | where keys live, PKCS12, HKDF, and key rotation | [Watch](https://youtu.be/XLj4BZuN7qc) |

### 5. Networking: what travels between machines

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 20 | [tcp-ip-basics](beginner/networking/tcp-ip-basics) | Plain | packets, the handshake, SYN floods, and sequence numbers | [Watch](https://youtu.be/7xt2gTZMaY4) |
| 21 | [tcp-ip-basics-with-tcpdump](beginner/networking/tcp-ip-basics-with-tcpdump) | With tools: tcpdump and Python | capture files and reading them | [Watch](https://youtu.be/M4zxQZ1uISQ) |
| 22 | [dns-basics](beginner/networking/dns-basics) | Plain | how names become addresses, and how that is forged | [Watch](https://youtu.be/o4AafF1wP2w) |
| 23 | [dns-basics-with-coredns](beginner/networking/dns-basics-with-coredns) | With tools: CoreDNS | a real name server in Docker | [Watch](https://youtu.be/tenRjrRNLt0) |
| 24 | [http-basics-and-headers](beginner/networking/http-basics-and-headers) | Plain | requests, cookies and security headers | [Watch](https://youtu.be/FNdaP39GY3s) |
| 25 | [http-basics-and-headers-with-spring-boot](beginner/networking/http-basics-and-headers-with-spring-boot) | With tools: Spring Boot | headers in a framework | [Watch](https://youtu.be/VI7lB_5psi0) |
| 26 | [http-basics-and-headers-with-nginx](beginner/networking/http-basics-and-headers-with-nginx) | With tools: nginx | headers and limits at the edge | [Watch](https://youtu.be/2Bu9JzS7tsg) |

### 6. Transport security: protecting the connection

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 27 | [tls-handshake](beginner/transport-security/tls-handshake) | Plain | how a secure channel is agreed | [Watch](https://youtu.be/WBjiXuvTbnc) |
| 28 | [tls-handshake-with-openssl](beginner/transport-security/tls-handshake-with-openssl) | With tools: OpenSSL | the other end of the handshake | [Watch](https://youtu.be/DmNUjiDX7RI) |
| 29 | [certificates-and-chain-of-trust](beginner/transport-security/certificates-and-chain-of-trust) | Plain | who vouches for a key | [Watch](https://youtu.be/wYccJS2oZGg) |
| 30 | [certificates-and-chain-of-trust-with-bouncy-castle](beginner/transport-security/certificates-and-chain-of-trust-with-bouncy-castle) | With tools: Bouncy Castle and OpenSSL | extensions, name constraints, CRLs | [Watch](https://youtu.be/8wZfw3zjubY) |
| 31 | [https-server](beginner/transport-security/https-server) | Plain | putting it together | [Watch](https://youtu.be/m2ZHjDrh6I4) |
| 32 | [https-server-with-spring-boot](beginner/transport-security/https-server-with-spring-boot) | With tools: Spring Boot | SSL bundles, reload and mutual TLS | [Watch](https://youtu.be/c5jOgFGfIx0) |
| 33 | [https-server-with-nginx](beginner/transport-security/https-server-with-nginx) | With tools: nginx | TLS termination and the forwarded-protocol trap | [Watch](https://youtu.be/nGpP7UxuuU4) |

### 7. Safe habits: what keeps a system safe over time

| # | Project | Kind | You learn | Video |
| ---: | --- | --- | --- | --- |
| 34 | [least-privilege](beginner/safe-habits/least-privilege) | Plain | giving only the access needed | [Watch](https://youtu.be/t1BZRhfXesU) |
| 35 | [least-privilege-with-spring-security](beginner/safe-habits/least-privilege-with-spring-security) | With tools: Spring Security | method security and blast radius | [Watch](https://youtu.be/lUJA2z1eC0o) |
| 36 | [least-privilege-with-kubernetes-rbac](beginner/safe-habits/least-privilege-with-kubernetes-rbac) | With tools: Kubernetes RBAC on kind | roles, bindings and a linter | [Watch](https://youtu.be/e-zE0OYedgg) |
| 37 | [patching-and-dependency-hygiene](beginner/safe-habits/patching-and-dependency-hygiene) | Plain | SBOMs, CVSS and patch windows | [Watch](https://youtu.be/1lzRAqPdmDU) |
| 38 | [patching-and-dependency-hygiene-with-trivy](beginner/safe-habits/patching-and-dependency-hygiene-with-trivy) | With tools: Trivy | scanning and dependency verification | [Watch](https://youtu.be/Pd-GP8bwozU) |
| 39 | [backups-and-recovery](beginner/safe-habits/backups-and-recovery) | Plain | surviving loss and ransomware | [Watch](https://youtu.be/zOytrYid8nA) |
| 40 | [backups-and-recovery-with-rustfs](beginner/safe-habits/backups-and-recovery-with-rustfs) | With tools: RustFS (S3 compatible) | versioning, object lock, write-only identity | [Watch](https://youtu.be/sKdqRdGlLJc) |
| 41 | [least-privilege-with-terraform](beginner/safe-habits/least-privilege-with-terraform) | With tools: OpenTofu and LocalStack | IAM policies as code | [Watch](https://youtu.be/0UjzvxeFiaw) |

## Intermediate: applied security

Track index: [intermediate](intermediate/README.md). Plan, with the OWASP, ASVS, CWE and RFC references each project maps to: [intermediate plan](../docs/intermediate-plan.md).

### 1. web-security: injection and access control (OWASP A01, A05)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 42 | [broken-access-control](intermediate/web-security/broken-access-control) | Plain | Signed in is not the same as allowed | A01, API1, CWE-639, CWE-862 | [Watch](https://youtu.be/7tARXr10wdU) |
| 43 | [broken-access-control-with-spring-security](intermediate/web-security/broken-access-control-with-spring-security) | With tools: spring security | A framework enforces exactly the rules it is given | A01, API1, CWE-639, CWE-862 | [Watch](https://youtu.be/UvxR-mH6g-s) |
| 44 | [sql-injection](intermediate/web-security/sql-injection) | Plain | Text typed by a customer must never become code | A05, CWE-89 | [Watch](https://youtu.be/98SbOdoLFyE) |
| 45 | [sql-injection-with-hibernate-and-postgres](intermediate/web-security/sql-injection-with-hibernate-and-postgres) | With tools: hibernate and postgres | Hibernate does not make a query safe: only parameters do, and a database user with few rights limits the damage of a mistake | A05, CWE-89 | [Watch](https://youtu.be/arWayzdbo_M) |
| 46 | [cross-site-scripting](intermediate/web-security/cross-site-scripting) | Plain | Text from a customer must stay text | A05, CWE-79 | [Watch](https://youtu.be/36GDr5NWx0g) |
| 47 | [cross-site-scripting-with-spring-boot](intermediate/web-security/cross-site-scripting-with-spring-boot) | With tools: spring boot | Let the template escape, and know the one switch that stops it | A05, CWE-79 | [Watch](https://youtu.be/vN_Ho9R1dTQ) |
| 48 | [cross-site-request-forgery](intermediate/web-security/cross-site-request-forgery) | Plain | A cookie proves who is asking, not what they meant | A01, CWE-352 | [Watch](https://youtu.be/we3U-gDTEvA) |
| 49 | [cross-site-request-forgery-with-spring-security](intermediate/web-security/cross-site-request-forgery-with-spring-security) | With tools: spring security | Spring Security's token check is on by default, and the ways to lose it are a switch, an exemption and a GET | A01, CWE-352 | [Watch](https://youtu.be/Q4_a12nAFkY) |
| 50 | [server-side-request-forgery](intermediate/web-security/server-side-request-forgery) | Plain | When the server fetches an address for a customer, it fetches it from inside | A01, CWE-918 | [Watch](https://youtu.be/I6-Dh7LBHuE) |
| 51 | [server-side-request-forgery-with-spring-boot](intermediate/web-security/server-side-request-forgery-with-spring-boot) | With tools: spring boot | A Spring RestClient fetches whatever it is told, so the address must be judged after resolution and the connection must go to the address that was judged | A01, CWE-918 | [Watch](https://youtu.be/oHKCCE0mjtA) |
| 52 | [xml-and-template-injection](intermediate/web-security/xml-and-template-injection) | Plain | A file or a template is a program for its reader | A05, CWE-611, CWE-1336 | [Watch](https://youtu.be/pSGxjphORuU) |
| 53 | [insecure-deserialization](intermediate/web-security/insecure-deserialization) | Plain | Rebuilding an object from bytes runs code the sender chose | A08, CWE-502 | [Watch](https://youtu.be/4pBE_rPVDWA) |
| 54 | [insecure-deserialization-with-jackson](intermediate/web-security/insecure-deserialization-with-jackson) | With tools: jackson | JSON is data until a type id tells Jackson which class to build, and then the cookie is choosing code | A08, CWE-502 | [Watch](https://youtu.be/R_ZK1T1Snug) |
| 55 | [path-traversal-and-file-upload](intermediate/web-security/path-traversal-and-file-upload) | Plain | A file name from a customer is a path until proven otherwise | A01, CWE-22, CWE-434, CWE-601 | [Watch](https://youtu.be/tqVemVY_dvM) |
| 56 | [path-traversal-and-file-upload-with-spring-boot](intermediate/web-security/path-traversal-and-file-upload-with-spring-boot) | With tools: spring boot | Spring hands you the file name exactly as the customer sent it, and its static handler follows a link out of the folder | A01, CWE-22, CWE-434, CWE-601 | [Watch](https://youtu.be/7qAQ3sr3434) |

### 2. secure-configuration: misconfiguration and exceptional conditions (OWASP A02, A10)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 57 | [security-misconfiguration](intermediate/secure-configuration/security-misconfiguration) | Plain | The settings nobody changed are the first thing an attacker checks | A02, CWE-16 | [Watch](https://youtu.be/W63hUHdjeEc) |
| 58 | [security-misconfiguration-with-spring-boot-actuator](intermediate/secure-configuration/security-misconfiguration-with-spring-boot-actuator) | With tools: spring boot actuator | One line that exposes every actuator endpoint hands a stranger the environment, the route map and the memory of the store | A02, CWE-16 | [Watch](https://youtu.be/zD-zMCYWHDQ) |
| 59 | [fail-closed-error-handling](intermediate/secure-configuration/fail-closed-error-handling) | Plain | When something breaks, the answer must be no | A10, CWE-636, CWE-755 | [Watch](https://youtu.be/8KLiydn_aKQ) |
| 60 | [fail-closed-error-handling-with-spring-boot](intermediate/secure-configuration/fail-closed-error-handling-with-spring-boot) | With tools: spring boot | A catch-all that returns yes, a transaction that does not roll back and an error page that names the server are all one Spring Boot default away | A10, CWE-636, CWE-755 | [Watch](https://youtu.be/72e2z8Zo_Xw) |

### 3. api-security: APIs and sessions (OWASP API Top 10)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 61 | [api-security](intermediate/api-security/api-security) | Plain | An API owes its callers three limits: what they may set, what they may see, and how much they may ask for | API3, API4, API6 | [Watch](https://youtu.be/Bqq-qQnlTsY) |
| 62 | [api-security-with-spring-boot-and-redis](intermediate/api-security/api-security-with-spring-boot-and-redis) | With tools: spring boot and redis | Bind a body to the stored record and a caller sets their own role; return the record and they read everyone's; count nothing and they never stop | API3, API4, API6 | [Watch](https://youtu.be/ZrLEr-xHyvI) |
| 63 | [session-management](intermediate/api-security/session-management) | Plain | A session id is a password that the server hands out | A07, CWE-384 | [Watch](https://youtu.be/5Gr1_bACSWs) |
| 64 | [session-management-with-spring-session-and-redis](intermediate/api-security/session-management-with-spring-session-and-redis) | With tools: spring session and redis | Moving sessions into Redis makes them survive a restart and a second server, and makes Redis the place where a stolen key is a live session | A07, CWE-384 | [Watch](https://youtu.be/0UT3pe-isds) |
| 65 | [cors](intermediate/api-security/cors) | Plain | CORS decides who may read an answer, not who may send a request | A02, CWE-942 | [Watch](https://youtu.be/ZgDZP0k1Cpo) |
| 66 | [cors-with-spring-boot](intermediate/api-security/cors-with-spring-boot) | With tools: spring boot | Spring's CORS answers 403 before your code runs, refuses a wildcard with credentials, and still reflects every origin if you ask it to | A02, CWE-942 | [Watch](https://youtu.be/ifwUPYDIYsE) |
| 67 | [webhook-security](intermediate/api-security/webhook-security) | Plain | A message to a public address proves nothing until it is verified | API8, API10, CWE-345 | [Watch](https://youtu.be/Cp8GHnzwbjw) |

### 4. identity-and-access: modern authentication and authorization (OWASP A07, RFC 9700)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 68 | [jwt-done-right](intermediate/identity-and-access/jwt-done-right) | Plain | Decoding a token is not verifying it | A07, CWE-347 | [Watch](https://youtu.be/3g8ewaqGqFM) |
| 69 | [jwt-done-right-with-nimbus-jose-jwt](intermediate/identity-and-access/jwt-done-right-with-nimbus-jose-jwt) | With tools: nimbus jose jwt | A standard library does the parsing, the signature and most claims; the algorithm, the type, the key set URL and the policy are still the API's to set | A07, CWE-347 | [Watch](https://youtu.be/slZVutWcUts) |
| 70 | [oauth2-authorization-code-pkce](intermediate/identity-and-access/oauth2-authorization-code-pkce) | Plain | Give an app access without giving it your password | RFC 9700, A07 | [Watch](https://youtu.be/mRfpQS047QU) |
| 71 | [oauth2-authorization-code-pkce-with-keycloak](intermediate/identity-and-access/oauth2-authorization-code-pkce-with-keycloak) | With tools: keycloak | The same flow against a real authorization server: the checks that make it safe are settings on the client, and Keycloak enforces them | RFC 9700, A07 | [Watch](https://youtu.be/9DcmwpRLldM) |
| 72 | [openid-connect](intermediate/identity-and-access/openid-connect) | Plain | Access is not identity | A07 | [Watch](https://youtu.be/80end6BSCag) |
| 73 | [openid-connect-with-spring-security-and-keycloak](intermediate/identity-and-access/openid-connect-with-spring-security-and-keycloak) | With tools: spring security and keycloak | Spring Security checks the ID token and keeps the state and nonce; what it leaves to the store is who the customer is, and how sign-out reaches every session | A07 | [Watch](https://youtu.be/ulqfRySf5ag) |
| 74 | [tokens-in-the-browser](intermediate/identity-and-access/tokens-in-the-browser) | Plain | A token a script can read is a token an attacker can take | OAuth for browser-based apps, RFC 9449 | [Watch](https://youtu.be/fxoPOQmZ4Gc) |
| 75 | [tokens-in-the-browser-with-spring-boot](intermediate/identity-and-access/tokens-in-the-browser-with-spring-boot) | With tools: spring boot | Keep the tokens on the server with a Spring Boot backend for the frontend, and bind any token the browser must hold to a key | OAuth for browser-based apps, RFC 9449 | [Watch](https://youtu.be/UBaGCWzQuzU) |
| 76 | [passkeys](intermediate/identity-and-access/passkeys) | Plain | A key that only signs for the real site | A07, NIST SP 800-63B | [Watch](https://youtu.be/DEixsAMtLOg) |
| 77 | [passkeys-with-spring-security](intermediate/identity-and-access/passkeys-with-spring-security) | With tools: spring security | Spring Security 7 gives a bank passkey registration and sign-in in a few lines; user verification, clone detection and the password you keep are still the bank's to decide | A07, NIST SP 800-63B | [Watch](https://youtu.be/LMvsLw_5OLw) |
| 78 | [saml-basics](intermediate/identity-and-access/saml-basics) | Plain | A valid signature is not the same as the statement you believed | A07, CWE-347 | [Watch](https://youtu.be/HyzGVMMGmUQ) |
| 79 | [saml-basics-with-keycloak](intermediate/identity-and-access/saml-basics-with-keycloak) | With tools: keycloak | A SAML service provider against a real identity provider: the library refuses every signature that is not the bank's, and a key rotation at the bank locks the shop out | A07, CWE-347 | [Watch](https://youtu.be/W6doaTTue_w) |

### 5. tls-in-practice

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 80 | [local-certificate-authority](intermediate/tls-in-practice/local-certificate-authority) | Plain | A certificate is a signed claim about a name, and revocation is only as good as the list the client has | A04 | [Watch](https://youtu.be/Qsa0yfBM44U) |
| 81 | [local-certificate-authority-with-step-ca](intermediate/tls-in-practice/local-certificate-authority-with-step-ca) | With tools: step ca | Run a real certificate authority for the store's own services: tokens and ACME, a policy for names, day-long certificates that renew, and a revocation list that only helps clients that read it | A04 | [Watch](https://youtu.be/YD8DvMe-AhE) |
| 82 | [tls-servers-in-practice-with-nginx-and-caddy](intermediate/tls-in-practice/tls-servers-in-practice-with-nginx-and-caddy) | With tools: nginx and caddy | The same shop behind nginx, where every TLS choice is a line you write, and behind Caddy, which makes its own certificates and renews them | A02, A04 | [Watch](https://youtu.be/19TfK61n-nA) |
| 83 | [tls-misconfigurations](intermediate/tls-in-practice/tls-misconfigurations) | Plain | A server's TLS is a set of choices, and a checker can read all of them | A02, A04 | [Watch](https://youtu.be/oVYiJ8c_DKo) |
| 84 | [tls-misconfigurations-with-testssl](intermediate/tls-in-practice/tls-misconfigurations-with-testssl) | With tools: testssl | Point testssl.sh at servers that are right and servers that are wrong, and turn what it reports into a gate that the shop owns | A02, A04 | [Watch](https://youtu.be/QcZ80IS6PKw) |
| 85 | [inspecting-tls-traffic](intermediate/tls-in-practice/inspecting-tls-traffic) | Plain | A key log opens exactly the connections it lists, and a leaked server key opens nothing that was ephemeral | A04 | [Watch](https://youtu.be/p2UUCWSsE2o) |
| 86 | [inspecting-tls-traffic-with-wireshark](intermediate/tls-in-practice/inspecting-tls-traffic-with-wireshark) | With tools: wireshark | A capture shows a name, times and sizes, a key log opens exactly the connections it lists, and a leaked server key opens only what never used an ephemeral key | A04 | [Watch](https://youtu.be/V-8sKltd8zo) |
| 87 | [certificate-pinning](intermediate/tls-in-practice/certificate-pinning) | Plain | A pin trusts one key instead of every authority, and the price is paid on the day the key changes | A04, CWE-295 | [Watch](https://youtu.be/OW1wQYs7IMs) |

### 6. secure-coding

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 88 | [input-validation](intermediate/secure-coding/input-validation) | Plain | Decide what is allowed, at the boundary, before it costs anything | A05, CWE-20, CWE-1333 | [Watch](https://youtu.be/mB7HwQ2dMRo) |
| 89 | [input-validation-with-hibernate-validator](intermediate/secure-coding/input-validation-with-hibernate-validator) | With tools: hibernate validator | Write what is allowed once, as annotations on the type, and let Jakarta Validation run it at the boundary, in an order, with limits first | A05, CWE-20, CWE-1333 | [Watch](https://youtu.be/91K_MSk8w6E) |
| 90 | [race-conditions-and-business-logic](intermediate/secure-coding/race-conditions-and-business-logic) | Plain | Correct one at a time is not correct at the same time | A06, CWE-362, CWE-840 | [Watch](https://youtu.be/kZ3jHWfjQAo) |
| 91 | [race-conditions-and-business-logic-with-postgres](intermediate/secure-coding/race-conditions-and-business-logic-with-postgres) | With tools: postgres | Two requests meet in the gap between a read and a write; the database can close it with a check, a lock, an isolation level or a version, and each has a price | A06, CWE-362, CWE-840 | [Watch](https://youtu.be/naAqHv0dH-g) |
| 92 | [secrets-in-errors-and-logs](intermediate/secure-coding/secrets-in-errors-and-logs) | Plain | A log is a second database that nobody protects | A09, CWE-209, CWE-532, CWE-117 | [Watch](https://youtu.be/kYQDD9J8Gew) |
| 93 | [secrets-in-errors-and-logs-with-logback](intermediate/secure-coding/secrets-in-errors-and-logs-with-logback) | With tools: logback | A log is a second database that nobody protects; make secrets unprintable first, cover the shapes you know second, and write events as data | A09, CWE-209, CWE-532, CWE-117 | [Watch](https://youtu.be/Wz85VJb7Li8) |
| 94 | [fuzzing](intermediate/secure-coding/fuzzing) | Plain | Let the machine try the inputs nobody thought of | SSDF PW.8 | [Watch](https://youtu.be/XYZlx0D9CoY) |
| 95 | [fuzzing-with-jazzer](intermediate/secure-coding/fuzzing-with-jazzer) | With tools: jazzer | Give the machine your example inputs and a fuzz target of three lines; it adds its own coverage counters, mutates the examples and reports the inputs nobody thought of | SSDF PW.8 | [Watch](https://youtu.be/zUngiZc4JQo) |

### 7. secrets-and-supply-chain (OWASP A03, A08; SLSA)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 96 | [secrets-management](intermediate/secrets-and-supply-chain/secrets-management) | Plain | A secret that never sits still is a secret that leaks less | A02, CWE-798 | [Watch](https://youtu.be/3toXMw_3zbI) |
| 97 | [secrets-management-with-openbao](intermediate/secrets-and-supply-chain/secrets-management-with-openbao) | With tools: openbao | A real secrets server hands each application its own short-lived database login, and takes it back when the lease ends or a leak is noticed | A02, CWE-798 | [Watch](https://youtu.be/VS7yNb2mkF8) |
| 98 | [artifact-signing](intermediate/secrets-and-supply-chain/artifact-signing) | Plain | Run only what the release team signed, and only what the world can see they signed | A08, SLSA | [Watch](https://youtu.be/MebeKSUFaBQ) |
| 99 | [artifact-signing-with-cosign](intermediate/secrets-and-supply-chain/artifact-signing-with-cosign) | With tools: cosign | A tag is a name, a digest is a checksum, and a signature on the digest is the only thing a deployment gate can rely on | A08, SLSA | [Watch](https://youtu.be/stUJnsuMBgg) |
| 100 | [sbom-and-provenance](intermediate/secrets-and-supply-chain/sbom-and-provenance) | Plain | Know what is inside a release, and prove where it came from | A03, SLSA, SSDF | [Watch](https://youtu.be/iNIB9UDPDss) |
| 101 | [sbom-and-provenance-with-syft-and-grype](intermediate/secrets-and-supply-chain/sbom-and-provenance-with-syft-and-grype) | With tools: syft and grype | A bill of materials says what is inside an image, a scanner says which of it is known to be broken, and a gate decides what to do about it | A03, SLSA, SSDF | [Watch](https://youtu.be/LW1S_DCOVR8) |
| 102 | [dependency-confusion](intermediate/secrets-and-supply-chain/dependency-confusion) | Plain | If a build can find your private name in a public place, someone else will put a package there | A03, CWE-427 | [Watch](https://youtu.be/0CmHIdO541g) |
| 103 | [dependency-confusion-with-gradle](intermediate/secrets-and-supply-chain/dependency-confusion-with-gradle) | With tools: gradle | Gradle asks every repository for every name unless it is told which repository may answer for which group, and then it checks the bytes against a list | A03, CWE-427 | [Watch](https://youtu.be/kGg1bytuTzE) |

### 8. platform-hardening

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 104 | [linux-hardening](intermediate/platform-hardening/linux-hardening) | Plain | Most host compromises begin with a setting that nobody looked at | CIS Benchmarks | [Watch](https://youtu.be/1hAvyPKWt1A) |
| 105 | [linux-hardening-with-docker](intermediate/platform-hardening/linux-hardening-with-docker) | With tools: docker | Scan a real container the way an auditor scans a host, fix what a machine can fix, and put every decision where it lasts: in the recipe of the image | CIS Benchmarks | [Watch](https://youtu.be/DXcUDtuGEGg) |
| 106 | [container-hardening](intermediate/platform-hardening/container-hardening) | Plain | Assume the process is taken over, and ask what it can reach | CIS Docker and Kubernetes Benchmarks | [Watch](https://youtu.be/--KLIPLbmPE) |
| 107 | [container-hardening-with-docker-and-kind](intermediate/platform-hardening/container-hardening-with-docker-and-kind) | With tools: docker and kind | Assume the process is taken over, and measure with thirteen real attempts what each docker run option and each Pod Security Standard takes away from it | CIS Docker and Kubernetes Benchmarks | [Watch](https://youtu.be/8vOcxQNzcPs) |

### 9. detection-and-response (OWASP A09; NIST CSF 2.0)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 108 | [security-logging](intermediate/detection-and-response/security-logging) | Plain | A log is only useful if it can answer who did it, and only safe if it holds no secrets | A09, ASVS V16 | [Watch](https://youtu.be/Iv__S6_L-yg) |
| 109 | [security-logging-with-loki-and-grafana](intermediate/detection-and-response/security-logging-with-loki-and-grafana) | With tools: loki and grafana | Ship the bank's security events to a real Loki, ask who moved the money in one line of LogQL, and let a rule watch for password guessing | A09, ASVS V16 | [Watch](https://youtu.be/X5QGdcqWB68) |
| 110 | [detecting-attacks](intermediate/detection-and-response/detecting-attacks) | Plain | A detection rule is a hypothesis about an attacker, and it must be measured against a normal day | A09, CSF Detect | [Watch](https://youtu.be/U2mDUk_NPD8) |
| 111 | [detecting-attacks-with-opensearch](intermediate/detection-and-response/detecting-attacks-with-opensearch) | With tools: opensearch | The same four detections as saved queries and monitors in a real OpenSearch, scored against one Monday of logs, and the one detection a query cannot say | A09, CSF Detect | [Watch](https://youtu.be/OELriMqm9Ho) |
| 112 | [incident-response](intermediate/detection-and-response/incident-response) | Plain | The outcome of an incident is mostly decided before it starts, and by the order of the first hour | CSF Respond, NIST SP 800-61 | [Watch](https://youtu.be/ykhqroUy4J0) |

### 10. secure-design (OWASP A06; SSDF)

| # | Project | Kind | You learn | Maps to | Video |
| ---: | --- | --- | --- | --- | --- |
| 113 | [threat-modeling](intermediate/secure-design/threat-modeling) | Plain | Find the threats on paper, before an attacker finds them in production | A06, SSDF PW.1 | [Watch](https://youtu.be/Pgr3ImXwPy4) |
| 114 | [threat-modeling-with-pytm](intermediate/secure-design/threat-modeling-with-pytm) | With tools: pytm | Describe the bank once, let pytm check its 113 threats against every element and flow, and watch each honest answer remove findings | A06, SSDF PW.1 | [Watch](https://youtu.be/0AK1CPx9lDQ) |
| 115 | [attack-trees-and-abuse-cases](intermediate/secure-design/attack-trees-and-abuse-cases) | Plain | Draw how the attacker wins, price each step, then cut the cheapest path and look again | A06 | [Watch](https://youtu.be/ESZeiL2dB8o) |
| 116 | [attack-trees-and-abuse-cases-with-threat-dragon](intermediate/secure-design/attack-trees-and-abuse-cases-with-threat-dragon) | With tools: threat dragon | Put the bank's abuse cases in a model file that OWASP Threat Dragon opens, check it with the editor's own validator, and let the plan turn threats from Open to Mitigated | A06 | [Watch](https://youtu.be/15lSZy6eIQs) |
| 117 | [security-requirements-with-asvs](intermediate/secure-design/security-requirements-with-asvs) | Plain | Turn a verification standard into requirements that check themselves | ASVS 5.0 | [Watch](https://youtu.be/kx12-5cF7ps) |

## Advanced: specialized topics

Rows with a link are built; the others are *planned*. Plan: [advanced plan](../docs/advanced-plan.md).

### 1. cloud-security-without-a-cloud

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 118 | [cloud-iam-models](advanced/cloud-security-without-a-cloud/cloud-iam-models) | Plain | Evaluate cloud permission policies the way the provider does: deny wins, accounts must agree, roles narrow, and a deputy can be confused | Java only (an emulator comes with cloud-misconfigurations) | [Watch](https://youtu.be/uAvjfLFuFFw) |
| 119 | [network-isolation](advanced/cloud-security-without-a-cloud/network-isolation) | With tools | Measure what a taken-over service can reach on a flat network, on coarse networks and on a layout built from the design, and compare the measurement with the intent | Docker networks, kind with network policies | [Watch](https://youtu.be/g_FN4nlXKoU) |
| 120 | [cloud-misconfigurations](advanced/cloud-security-without-a-cloud/cloud-misconfigurations) | With tools | Deploy a bank's first cloud setup to an AWS emulator, find 13 misconfigurations, fix them, and find the same problems in the code before deploy with Checkov and Trivy | Moto (LocalStack now needs an account token), Checkov, Trivy | [Watch](https://youtu.be/-lIDBBKKOZw) |

### 2. infrastructure-as-code-security

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 121 | [iac-scanning](advanced/infrastructure-as-code-security/iac-scanning) | With tools | Write eight rules for Kubernetes manifests with tests of their own, run Checkov, Trivy and KICS on the same files, and handle exceptions and a baseline in the build | Checkov, Trivy, KICS | [Watch](https://youtu.be/tUFDhfNWmTE) |
| 122 | [policy-as-code](advanced/infrastructure-as-code-security/policy-as-code) | With tools | Write a bank's deny-by-default deployment policy in Rego, test the policy and test its tests, and run it as an admission review and in the build with Conftest | OPA and Rego, Conftest | [Watch](https://youtu.be/JRe5HDJ06Zk) |

### 3. kubernetes-and-container-security

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 123 | [kubernetes-admission-control](advanced/kubernetes-and-container-security/kubernetes-admission-control) | With tools | Stop a bank's unsafe workloads at the door of a real Kubernetes cluster with Kyverno policies that validate, mutate, verify signed images and take exceptions | kind, Kyverno, cosign | [Watch](https://youtu.be/_pdLQMOegNE) |
| 124 | [kubernetes-network-policies](advanced/kubernetes-and-container-security/kubernetes-network-policies) | With tools | Default-deny a bank's pods on a real Kubernetes cluster, allow only what the design lists, and measure every pair of pods against the design | kind, kindnet | [Watch](https://youtu.be/PXZD4B92OGg) |
| 125 | [runtime-security](advanced/kubernetes-and-container-security/runtime-security) | With tools | Detect a shell, an unexpected write and an unexpected outbound connection in a bank's payments pod on a real Kubernetes cluster, with Falco's rules and the Linux audit trail | kind, Falco rules, Linux audit | [Watch](https://youtu.be/YWdsHQ0leBc) |
| 126 | [kubernetes-secrets](advanced/kubernetes-and-container-security/kubernetes-secrets) | With tools | Why a Kubernetes Secret is only base64, how to read it raw out of etcd before and after encryption at rest, and how OpenBao and a bound token replace a long-lived secret | kind, OpenBao | [Watch](https://youtu.be/SoaJZzn93JA) |

### 4. devsecops-pipelines

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 127 | [sast](advanced/devsecops-pipelines/sast) | With tools | Write static analysis rules for a bank's Java code, compare a pattern rule with a taint rule, test the rules, and triage the findings with Semgrep | Semgrep | [Watch](https://youtu.be/CsdUUm9crp8) |
| 128 | [dast](advanced/devsecops-pipelines/dast) | With tools | Scan a running bank web application with OWASP ZAP, compare a passive, an anonymous and a logged-in scan with the flaws that were planted, and rescan the fixed version | OWASP ZAP | [Watch](https://youtu.be/0NESp1n8mLs) |
| 129 | [secret-scanning](advanced/devsecops-pipelines/secret-scanning) | With tools | Find a bank's secrets in the files and in the whole git history, compare a scanner you write with gitleaks, and block the next secret before it leaves the laptop | gitleaks, git hooks | [Watch](https://youtu.be/n2ewD6_nIqs) |
| 130 | [secure-pipeline](advanced/devsecops-pipelines/secure-pipeline) | With tools | A release pipeline for a bank's Gradle library with reproducible builds, cosign signatures, SLSA-style provenance and gates that fail closed, and a deploy step that refuses forged releases | Gradle, cosign, trivy, gitleaks | [Watch](https://youtu.be/p42WbnAc88c) |

### 5. service-identity-and-zero-trust

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 131 | [mutual-tls-between-services](advanced/service-identity-and-zero-trust/mutual-tls-between-services) | With tools | Make a bank's services prove who they are with client certificates from a private CA, authorize by that identity and not by the network, and rotate short-lived certificates without downtime | JDK TLS, step-ca | [Watch](https://youtu.be/-ZCrqyPonW0) |
| 132 | [service-mesh-mtls](advanced/service-identity-and-zero-trust/service-mesh-mtls) | With tools | Let a mesh give a bank's pods identities from their ServiceAccounts and encrypt every call, measure it on a real Linkerd cluster, and see what the mesh does not protect | kind, Linkerd | [Watch](https://youtu.be/3Rlwu9aunTw) |
| 133 | [workload-identity](advanced/service-identity-and-zero-trust/workload-identity) | With tools | Replace a shared secret with SPIFFE IDs and short-lived SVIDs from a real SPIRE server and agent, between two Java services at a bank | SPIRE, java-spiffe | [Watch](https://youtu.be/3FIdGWtuby0) |
| 134 | [zero-trust-access](advanced/service-identity-and-zero-trust/zero-trust-access) | With tools | Put an identity-aware proxy in front of a bank's ledger API: every request is authenticated with a Keycloak token, authorized by OPA and written to an audit log, close to the resource | Keycloak, OPA | [Watch](https://youtu.be/wJPluFm6Yfw) |

### 6. applied-cryptography-and-pki

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 135 | [certificate-lifecycle](advanced/applied-cryptography-and-pki/certificate-lifecycle) | With tools | Issue certificates from a private root and intermediate, revoke them with a CRL, an OCSP responder and a stapled response, replace them with short-lived certificates that step-ca renews, and watch what a missed renewal does | step-ca, OpenSSL | [Watch](https://youtu.be/jTvSXU0JjmY) |
| 136 | [tls-attacks-and-downgrades](advanced/applied-cryptography-and-pki/tls-attacks-and-downgrades) | With tools | Run deliberately weak local TLS servers, scan them with openssl and testssl.sh against the weaknesses that were planted, watch a client fall back to TLS 1.0, read a recorded session with a leaked key, and check what TLS 1.3 removed | OpenSSL, testssl.sh | [Watch](https://youtu.be/Wg6ngJbsZyU) |
| 137 | [post-quantum-cryptography](advanced/applied-cryptography-and-pki/post-quantum-cryptography) | With tools | Use ML-KEM and ML-DSA from the JDK, combine X25519 with ML-KEM in a hybrid exchange, measure what the new sizes cost, and count where a bank's code still depends on RSA and elliptic curves | JDK 25, Bouncy Castle | [Watch](https://youtu.be/S8CMCn7deSk) |
| 138 | [crypto-agility](advanced/applied-cryptography-and-pki/crypto-agility) | Plain | Inventory the algorithms in a bank's code, store data in a versioned envelope, and rotate the algorithm and the key under 10,100 records without losing one | JDK 25 | [Watch](https://youtu.be/9y_-HWwtjTw) |
| 139 | [hardware-backed-keys](advanced/applied-cryptography-and-pki/hardware-backed-keys) | With tools | Generate a bank's payment-signing key on a PKCS#11 token, sign with it from Java, try 5 ways to get the key out, and measure what the token does not protect against | SoftHSM, SunPKCS11 | [Watch](https://youtu.be/om_EwCH4YxI) |

### 7. detection-engineering

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 140 | [detection-as-code](advanced/detection-engineering/detection-as-code) | With tools | Write a bank's detections as Sigma rules tagged with MITRE ATT&CK, test them against a week of labelled logs, hold each to a false-positive budget, and run the pySigma-converted queries in OpenSearch | Sigma, pySigma, OpenSearch | [Watch](https://youtu.be/5W_vaFB16Q8) |
| 141 | [siem-basics](advanced/detection-engineering/siem-basics) | Plain | Collect a bank's web, database and host logs in three formats, normalize them into one shape and one clock, and correlate across sources to see an attack that no single source shows | Java | [Watch](https://youtu.be/WcKCEZIAX9g) |
| 142 | [threat-hunting](advanced/detection-engineering/threat-hunting) | Plain | Hunt through a simulated week of a bank's logs with four hypotheses, judge the leads against what really happened, and turn the hunt that worked into a rule | Java | [Watch](https://youtu.be/NtuekrsZ_ns) |

### 8. offensive-security-methodology

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 143 | [vulnerability-research](advanced/offensive-security-methodology/vulnerability-research) | With tools | Fuzz a bank's statement reader with Jazzer, take the crashes to their root causes, fix them, prove the fix, and write the advisory with a coordinated disclosure timeline | Jazzer, CVSS 3.1 | [Watch](https://youtu.be/oYXqVUlz6P4) |
| 144 | [reverse-engineering-fundamentals](advanced/offensive-security-methodology/reverse-engineering-fundamentals) | With tools | Read a bank client's class files with the JDK's class file API, javap and the CFR decompiler, find a hard-coded key, patch a licence check, and see what obfuscation does and does not stop | javap, CFR, the JDK class file API | [Watch](https://youtu.be/KJvoRFah9ek) |
| 145 | [malware-analysis-fundamentals](advanced/offensive-security-methodology/malware-analysis-fundamentals) | With tools | Triage harmless, purpose-built samples with hashes, strings, entropy and YARA rules, and run one in a disposable container to see what a sandbox stops | YARA, a disposable container | [Watch](https://youtu.be/8f8ZR2Pvtps) |

### 9. resilience-and-recovery

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 146 | [ransomware-resilience](advanced/resilience-and-recovery/ransomware-resilience) | With tools | Keep a bank's ledger backups where an attacker with the backup credentials cannot destroy them: object lock, separate credentials, a second store, a guard against poisoned backups, and a restore exercise with a recovery time and point | RustFS object lock, AWS SDK | [Watch](https://youtu.be/tPJ-FSiL8W8) |
| 147 | [security-chaos-engineering](advanced/resilience-and-recovery/security-chaos-engineering) | With tools | Inject failures into the security controls of a bank payments service with Toxiproxy and check that they fail closed and that somebody is told | Toxiproxy | [Watch](https://youtu.be/lbClD_Lbjrw) |

### 10. governance-risk-and-compliance

| # | Project | Kind | You learn | Tools | Video |
| ---: | --- | --- | --- | --- | --- |
| 148 | [risk-management](advanced/governance-risk-and-compliance/risk-management) | Plain | Quantify a bank's risk register with ranges and a Monte Carlo simulation, choose controls within a budget, and check the residual risk against an appetite | Java, Monte Carlo simulation | [Watch](https://youtu.be/hhyJrtVGLM4) |
| 149 | [controls-and-evidence](advanced/governance-risk-and-compliance/controls-and-evidence) | With tools | Map a bank's controls to NIST CSF 2.0 and ISO 27001, collect evidence from test results, and see which controls are passing, failing, stale or missing | Java, JUnit reports | [Watch](https://youtu.be/iS0ujT-JFTg) |
| 150 | [security-architecture-review](advanced/governance-risk-and-compliance/security-architecture-review) | With tools | Review a bank's design written in the Structurizr DSL with rules over trust zones, flows, data stores and decision records, and see what no rule can find | Structurizr DSL, ADRs | [Watch](https://youtu.be/4kTVlkBQkGI) |
| 151 | [privacy-engineering](advanced/governance-risk-and-compliance/privacy-engineering) | With tools | Minimize, pseudonymize, retain and erase a bank's customer data in PostgreSQL, measure who can still be picked out, and find the copies that an erasure request misses | Java, PostgreSQL | [Watch](https://youtu.be/mn-lg7bxkr8) |

## Other pages

- [YouTube playlists](playlists-youtube.md): every playlist, by level and module, with its videos.
- [YouTube uploads](../docs/youtube.md): every video's review and upload status.
- [Beginner learning path](beginner/learning-path.md): the beginner track on its own.
- [Repository README](../README.md): the scope of the whole repository.
