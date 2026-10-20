# A Local Certificate Authority

```
src/main/java/com/jk/explore/security/ca/
├── CaDemo.java        Runs the local certificate authority story in five numbered acts and prints exact, repeatable results
├── Certificates.java  Builds X.509 version 3 certificates and version 2 revocation lists as real DER, signed with ECDSA and SHA-256, and reads them back with the JDK
├── Der.java           The few ASN.1 DER encodings an X.509 certificate and a revocation list need
├── LocalCa.java       A small certificate authority: one root key, a serial number counter, a list of revoked serials, and a revocation list it can publish
└── Verifier.java      What a client does with a server's certificate: build the chain to a trusted root, check the dates and the revocation list at a given moment, check the name in the <em>subject alternative name</em> (never the common name), and check that the certificate may be used for a TLS server
```

**A certificate authority signs the claim that a key belongs to a name; a client trusts the authority's root, checks the chain, the dates, the name in the subject alternative name and a current revocation list, and short-lived certificates make most revocation unnecessary.**

A **certificate** is a signed statement: "this public key belongs to shop.example". The signer is a **certificate authority (CA)**, and a client that trusts the CA's **root certificate** can trust every certificate that CA signs.
This project builds a small CA in plain Java, with real **X.509 version 3 certificates** and **version 2 revocation lists (CRLs)** written as DER bytes by hand and read back by the JDK's own PKIX code, so every check is the one a real client performs.

What the CA does, and what a client does with it, in the order this project shows them:
- **Issue** a server certificate with a serial number, dates, a **subject alternative name** list and the *server authentication* usage; the CA key never leaves the CA.
- **Verify** like a client: the chain to a trusted root, the dates at a given moment, the signature, whether each certificate may sign others (**basic constraints**), the name in the SAN (never the common name), and the usage.
- **Revoke** a certificate and **publish a list**. A client with no current list refuses (**hard fail**); a client that accepts "no list" is trusting a stolen key.
- **Renew** before expiry, for the same key and names, with a new serial number.
- **Short-lived certificates** (hours, not months) cut the exposure window without any list.

**Maps to:** OWASP Top 10 (2025) A04 Cryptographic Failures · CWE-295 · CWE-299 · RFC 5280 (X.509 and CRL profile) · RFC 6125 (names in certificates) · NIST SP 800-57.

## The idea in everyday terms

A passport office stamps a passport that says this photo belongs to this name. A border guard trusts the office, not the traveller. The guard also checks that the passport is not expired, that the name matches, that the stamp is genuine, and that the passport
is not on this morning's list of lost ones. A passport that lasts one day would need no list.

## The scenario

A shop runs its own internal services on names like shop.example and wants TLS between them without a public CA. The demo builds a root CA, issues and renews certificates, and runs a client verifier against honest and dishonest certificates,
using a fixed clock so that expiry and revocation are exact.

## Run

```bash
./gradlew run
```

The demo tells the story in 5 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. Issue a certificate and trust the root | The CA issues a 90-day certificate for shop.example and *.shop.example, and the client that trusts the root accepts it. |
| 2. What a client refuses | A wildcard covers one label, not two; other names, expired, other-authority, forged, self-signed certificates and a leaf that signs another certificate are all refused. |
| 3. Revocation | On day 10 the key is stolen and the CA revokes serial 1000: the client refuses with the new list, and also with a stale list or no list at all. |
| 4. Renewal | Serial 1000 is renewed as serial 1001 with the same key; on day 100 the old certificate is expired and the renewed one is accepted. |
| 5. Short-lived certificates instead of revocation | With daily lists a stolen key works up to 24 hours after the theft is noticed; with 8-hour certificates it stops after at most 8 hours with no list; a nine-hour-old certificate is expired. |

## Test

```bash
./gradlew test
```

12 tests in `CaTest`, `DemoRunsTest`. Keys are random and never printed, serial numbers count from 1000, and the clock is fixed, so the demo prints the same text on every run.

## What the simulation got right, and what it left out

The certificates and lists are real DER built by the project's own small encoder and read back by `CertificateFactory`; chain and revocation checking is the JDK's PKIX `CertPathValidator`. Keys are ECDSA P-256 and are random; the clock is fixed so the output is exact.

## Technologies and versions

Every version below is pinned in the project's build files and was the latest stable release when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |

## Learning Material

| Document | What it is for |
| --- | --- |
| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |
| [Prerequisites](docs/prerequisites.md) | what you need to know first |
| [A Local Certificate Authority, explained](docs/local-certificate-authority-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

Clients are given the root once; after that every certificate the CA signs is trusted until it expires or is listed.

![A private CA and the clients that trust it](docs/images/architecture-diagram.png)

### Where each piece sits

The CA builds; the client verifies; the DER encoder is shared.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

Any failure ends the connection.

![What a client checks, in order](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![Issuing and using a certificate](docs/images/sequence-diagram.png)

## What it costs

- **The root key is the whole system.** Keep it offline and use an intermediate for daily issuing; this project keeps one key to stay small.
- **Revocation lists must be fetched and fresh.** A client that cannot get one either fails (safe, but an outage) or ignores it (unsafe). Browsers moved away from CRLs for this reason.
- **Short-lived certificates need automation:** renewal must be unattended and monitored, or short lives become outages.
- **Every client must be given the root,** and removing it later is hard. Scope it: one root for one purpose, and constrain names where the tools allow.
- **Wildcards widen the damage of one stolen key** to every name under the domain.

## When this is too much

For public websites use a public CA and ACME; do not run your own. Run a private CA for internal service-to-service TLS, device identity and tests, and prefer an existing tool (step-ca, Vault PKI) to this teaching code in production.

## Where you have already met this

- The padlock and "Certificate details" in a browser.
- `keytool -genkeypair`, `openssl req -x509` and `-----BEGIN CERTIFICATE-----`.
- "Your connection is not private" with NET::ERR_CERT_DATE_INVALID or a name mismatch.
- CRL and OCSP in the certificate's authority information.

## Where this sits

This project is in [tls-in-practice](..) and builds on the beginner [tls-handshake](../../../beginner/transport-security/tls-handshake) project, which explained certificates and the chain of trust from the client's side.
The with-step-ca version runs the same ideas against a real CA with ACME.
