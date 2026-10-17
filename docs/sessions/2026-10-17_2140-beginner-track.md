# Session log: building the beginner track

Saved: 2026-10-17 21:40 (local time). Branch `security_draft`, last pushed commit `f0be2f6`.

## Outcome

The beginner track is complete: 41 projects in seven modules, each with Java 25 code, tests, a golden demo output, docs,
diagrams, a threat model with a diagram, an animation and a narrated video. Everything is committed and pushed.
**Nothing has been uploaded anywhere.** Upload sheets exist, and uploading waits for the owner's review.

## Standing rules from the owner

- No uploads, scheduling or publishing until the owner has reviewed and asks.
- Commit messages: "Adding <project> - <description>". **No co-author line and no AI mention, ever.**
- Push only when asked.
- Domain: an online store or a bank only, whichever fits.
- All labs run locally. Any local tool is fine: Docker, kind, nginx, LocalStack, OpenTofu, Python, openssl, Spring, Tomcat,
  Hazelcast, Redis, Postgres, Hibernate, Vert.x, Ehcache.
- JDK 25. Latest stable versions. **No deprecated API anywhere:** every build runs `-Xlint:deprecation -Xlint:removal -Werror`.
- Every project documents its tech stack and pinned versions, and has a threat model with a diagram.
- Do not reference the sibling pattern project in any file.
- Up to three projects may be built in parallel, each by a separate agent touching only its own directory.

## Modules and projects, in learning order

| Module | Plain project | With-tools projects |
| --- | --- | --- |
| foundations | cia-triad, threats-and-risk | – |
| identity | authentication-vs-authorization | with-spring-security, with-keycloak |
| passwords-and-mfa | password-hashing, password-attacks-and-defences, totp-multi-factor | with-spring-security-crypto, with-nginx, with-java-otp |
| cryptography | hashing-and-integrity, symmetric-encryption, asymmetric-encryption, digital-signatures, keystore-basics | symmetric-encryption-with-tink, asymmetric-encryption-with-bouncy-castle, digital-signatures-with-bouncy-castle |
| networking | tcp-ip-basics, dns-basics, http-basics-and-headers | tcp-ip-basics-with-tcpdump, dns-basics-with-coredns, http-basics-and-headers-with-spring-boot, http-basics-and-headers-with-nginx |
| transport-security | tls-handshake, certificates-and-chain-of-trust, https-server | tls-handshake-with-openssl, certificates-and-chain-of-trust-with-bouncy-castle, https-server-with-spring-boot, https-server-with-nginx |
| safe-habits | least-privilege, patching-and-dependency-hygiene, backups-and-recovery | least-privilege-with-spring-security, least-privilege-with-kubernetes-rbac, least-privilege-with-terraform, patching-and-dependency-hygiene-with-trivy, backups-and-recovery-with-rustfs |

The ordered list with links is `java/beginner/learning-path.md` (and its `.html` twin). The plan and checklist are in `docs/beginner-plan.md`.

## Work done in this part of the session

Built after `keystore-basics`: tcp-ip-basics and its tcpdump version; dns-basics and its CoreDNS version; the three http-basics
projects; the TLS, certificate and HTTPS projects with their tools versions; the safe-habits projects; digital-signatures-with-bouncy-castle;
authentication-vs-authorization-with-keycloak; least-privilege-with-terraform. Also: the learning-path index, three module pages, a fix
of the JUnit version shown in six projects' docs, and a rename of the object-store project from MinIO to RustFS.

## Versions pinned (as of this session)

Gradle 9.8.1, JUnit 6.1.3, JDK 25 via toolchain. Spring Boot 4.1.1 (the 4.2 line was milestones only), Spring Framework 7.0.9,
Spring Security 7.1.1, Tomcat 11.0.24. Bouncy Castle 1.86, Tink 1.24.0, AWS SDK v2 2.56.0, SnakeYAML Engine 3.2, Jackson 3.2.3.
nginx 1.31.7-alpine (older nginx projects still use 1.31.6), CoreDNS 1.14.7, Keycloak 26.8.1, Trivy 0.76.0, RustFS 1.0.2,
LocalStack 4.14.0, OpenTofu 1.13.1 with AWS provider 6.69.1, kind 0.33.0, tcpdump 4.99.1, OpenSSL 3.6.3, Python 3.14.

## Decisions and gotchas worth remembering

- **MinIO no longer publishes container images**, so the object-store project uses RustFS and was renamed `-with-rustfs`.
- **LocalStack's latest image needs an auth token**; 4.14.0 is the last community tag that starts without one. The community emulator
  stores IAM policies but does not enforce them, so real denial is checked by the Java evaluator.
- **tcpdump prints no `seq` for a bare ACK**, and `-v` marks a bad checksum as "incorrect", not "bad cksum". The Java formatter matches the real tool.
- **Docker-dependent demos** (nginx, CoreDNS, RustFS, Keycloak) run under `./gradlew integrationTest`; the default `test` needs only a JDK.
  Other tool-dependent tests are tagged `openssl`, `tools`, `terraform` or `k8s`.
- **Spring Boot 4 ships Jackson 3**, so some APIs return plain strings instead of JSON maps.
- **Keycloak marks its cookies Secure over http**; Java's cookie manager then drops them on loopback, so the test client keeps its own jar.
- Rerunning `conceptkit docs` rewrites `animation.html` without its player; `conceptkit build` always puts the player back.
- zsh: write `"${var}:refs/..."` in refspecs, and give long-running shell loops patterns that do not match themselves.
- Pushes of large batches can fail with HTTP 408; pushing in batches of commits worked.

## Open items

- The owner's review of every project's video, animation and docs, one by one, before any upload.
- The first three repository commits carry co-author lines; removing them needs a history rewrite and was not done.
- Allowed but unused so far: Hazelcast, Redis, Postgres, Hibernate, Vert.x, Ehcache. Natural fits: a `-with-redis` rate limiter,
  `-with-postgres` row-level security, session stores, and cache topics.
- The next level above beginner is not planned yet.

## Resuming

Tools: `tools/conceptkit/conceptkit.sh` (`scaffold`, `docs`, `build [--no-media]`, `html`, `site`) and `tools/videokit/videokit.sh`.
Run Gradle with `-Dorg.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@25`.
Each project's `concept.toml` is the source for its README, docs, diagrams, animation and video scenes.
