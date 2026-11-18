# Workload Identity

```
src/main/java/com/jk/explore/security/workloadid/
├── Lab.java                   A real SPIRE server and agent in one Linux container, plus the Java workloads that run beside them as different operating system users
├── LegacyLedger.java          The "before": a ledger that trusts one static secret and the caller's own word about who it is
├── Policy.java                The ledger's authorization: which SPIFFE ID may call which route
├── Rotations.java             Counts how often a certificate was replaced, from the serial numbers seen one after the other
├── Sh.java                    Runs a program and returns what it printed, whether it worked or not
├── SpiffeIds.java             A teaching class: a SPIFFE ID taken apart by hand, the way the java-spiffe library reads it (a test checks they agree)
├── Workload.java              The program that runs inside the SPIRE container, once per workload
└── WorkloadIdentityDemo.java  A bank's ledger is called by payments and reporting
```

**One static secret copied into 3 services lets a leak from any one of them act as every service; with SPIRE each workload gets a SPIFFE ID chosen by a registration entry and its operating system user, not by what it claims, and an X.509-SVID that lives 20 seconds; two Java services verify each other's SPIFFE ID against the trust bundle, the ledger authorizes by that ID (200 and 403 as the rules say), the SVID is renewed on its own at least 3 times in 50 seconds with 0 calls failed, a stolen copy works while it lives and is refused after 20 seconds, and a process that matches no entry gets no SVID.**

A shared secret says "someone who holds this key is calling", never which service. This project first shows that **before**: one static key copied into the configuration of payments, reporting and fraud-engine; when the fraud-engine's copy leaks, the attacker is every service. It then runs a real **SPIRE** server and agent (SPIFFE's reference implementation) and gives each workload a **SPIFFE ID** such as `spiffe://bank.example/payments`, chosen by a registration entry and the operating system user the process runs as, plus an **X.509-SVID** that lives 20 seconds. SPIRE has no macOS build, so the official linux arm64 release runs in one Docker container (`eclipse-temurin:25-jre`, pinned by digest, the release downloaded once into `build/spire`); the Java workloads run in that container as different users and use the `java-spiffe` Workload API client. The demo is tagged `integration`: `./gradlew integrationTest` (about 3 minutes).

What it shows:
- **The shared secret.** Held by **3** services, it lets a copy from one service call as any of them; the ledger takes the caller's word for who it is.
- **Identity by registration.** The same Java program gets `ledger`, `payments` or `reporting` depending on the user it runs as; a claim in its environment changes nothing.
- **Mutual verification.** Two Java services check each other's SPIFFE ID against the trust bundle; the ledger answers 200 and 403 by the rules; a server with the wrong ID and a certificate made by an attacker are refused (0 requests reach the ledger).
- **Rotation.** SVIDs are replaced at least **3** times in 50 seconds with **0** failed calls and no renewal code in the services.
- **A stolen SVID.** It works while it lives and is refused after 20 seconds; a shared secret still works.
- **No entry, no identity.** A process that matches nothing gets no SVID, and deleting an entry ends a workload's identity.

**Maps to:** NIST SP 800-207 (zero trust) · SPIFFE and SPIRE · OWASP ASVS 5.0 V12 Secure Communication · CWE-798 (hard-coded credentials).

## The idea in everyday terms

A shared secret is a master key cut three times: whoever finds one lost copy can open every door and nobody can tell which copy it was. A SPIFFE ID is a name badge printed at the door by a guard who looks at who is standing there, and the badge stops working after 20 seconds.

## The scenario

A bank's ledger is called by payments (which moves money) and reporting (which reads statements). The demo first lets one shared key leak, then replaces it with SPIRE identities, rotates them, steals one and starts a process that matches no entry.

## Run

```bash
./gradlew run
```

The demo tells the story in 7 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. One shared secret in three services | One random key is copied into payments, reporting and fraud-engine (3 of 3). The ledger checks the key, then believes the X-Service header: reporting saying payments may POST /transfers (200). With fraud-engine's leaked copy an attacker outside the bank gets 200 as payments and 200 as reporting; only a wrong key gets 401. |
| 2. SPIRE decides who a workload is | A SPIRE 1.15.5 server and agent hold 3 registration entries: unix:uid:2001 -> spiffe://bank.example/ledger, 2002 -> payments, 2003 -> reporting. The same Java program run as each user gets that ID; run as user 2003 while saying it is payments, it still gets /reporting. An SVID's dates span 30 s: 10 s back-dated, so 30 - 10 = 20 s, the TTL. |
| 3. Two Java services check each other's SPIFFE ID | Over mutual TLS the ledger reads the caller's SPIFFE ID from its certificate and applies the same two rules: payments POST /transfers 200, GET /statements 403; reporting GET /statements 200, POST /transfers 403. The audit names 4 requests by certificate. A server run as reporting is refused by payments, which expects spiffe://bank.example/ledger, and a self-made certificate naming payments is refused before any request reaches the ledger (0). |
| 4. Rotation: an SVID that lives 20 seconds | payments calls the ledger on a new connection about 4 times a second for 50 s, 50 / 20 = 2.5 SVID lifetimes: at least 100 calls (yes), 0 failed, payments' own SVID and the ledger's as seen by payments each replaced at least 3 times (yes), with no renewal code in the services. |
| 5. A stolen SVID | The certificate, key and bundle an SVID helper left on payments' disk are copied by another user. With the copy the attacker's POST /transfers gets 200 and the audit names spiffe://bank.example/payments; after 21 s (> 20) it is refused, while payments itself gets 200 and the leaked shared secret still gets 200. |
| 6. A workload that matches no entry | A process run as the attacker's user 2004, saying it is payments, gets no SVID and cannot call the ledger (0 requests reach it). Once the payments entry is deleted, a new payments process gets no SVID either, while reporting still gets 200. |
| 7. Evicting the attacker | Changing the shared secret stops the leaked copy (401) but breaks 3 of 3 services until each gets the new one. The stolen SVID needed no change in any service (0): it expired by itself, while the leaked secret kept working (200) until it was changed. |

## Test

```bash
./gradlew test
```

13 tests in `DemoRunsTest`, `LegacyLedgerTest`, `PolicyTest`, `RotationsTest`, `SpiffeIdsTest`. The policy, the rotation count, the SPIFFE ID parts and the shared-secret ledger are tested with no tools. The demo runs SPIRE in Docker (./gradlew integrationTest).

## What the simulation got right, and what it left out

SPIRE (server and agent 1.15.5), Docker and the java-spiffe client are real, and every number is measured. The "bank" is a ledger and three callers on one node; a real deployment has many nodes and a node attestor stronger than a join token. The demo never prints ports, serial numbers, tokens or times.

## Technologies and versions

Every version below is pinned in the project's build files and was the latest stable release when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| io.spiffe:java-spiffe-provider | 0.8.17 | library the code uses |
| io.grpc:grpc-netty-shaded | 1.80.0 | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |

## Learning Material

| Document | What it is for |
| --- | --- |
| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |
| [Prerequisites](docs/prerequisites.md) | what you need to know first |
| [Workload Identity, explained](docs/workload-identity-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

A SPIRE server and agent give each Java workload an identity.

![The parts](docs/images/architecture-diagram.png)

### Where each piece sits

The rules, the lab, the workloads and the shared-secret ledger.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

The agent looks at who is asking, not at what it says.

![From a process to an identity](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![A call between two services](docs/images/sequence-diagram.png)

### Video

`video/workload-identity-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **SPIRE is more to run:** a server, an agent on each node and registration entries to keep.
- **The agent and the server must stay up,** because a short lifetime depends on renewal.
- **The identity is only as strong as the selector,** here the operating system user.
- **A stolen SVID works until it expires,** so the lifetime is the revocation.
- **Services need a Workload API client** and a TLS context that reads the SVID at each handshake.

## When this is too much

One service calling one other service inside a closed network may be fine with a managed secret that is rotated. Use workload identity when many services call each other and a leak from any one of them must not be worth everything.

## Where you have already met this

- An API key in an environment variable copied into every service.
- A service account password nobody has changed since it was created.
- A client certificate that lives for a year.
- A header that says which service is calling.

## Where this sits

This is a project of the [service-identity-and-zero-trust](..) module, with [mutual-tls-between-services](../mutual-tls-between-services) and [service-mesh-mtls](../service-mesh-mtls). The first runs its own CA; this one lets SPIRE decide who a workload is and renew its certificate.
