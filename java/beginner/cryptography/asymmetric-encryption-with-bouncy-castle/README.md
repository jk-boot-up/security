# Asymmetric Encryption with Bouncy Castle and OpenSSL

```
src/main/java/com/jk/explore/security/asymmetricbc/
├── BouncyCastleDemo.java  Runs the online bank story in five numbered acts, with Bouncy Castle and OpenSSL, and prints facts that do not depend on random keys
├── Certificates.java      Makes a self-signed X.509 certificate: a public key, a name, a validity period, all signed with the matching private key
├── CmsEnvelope.java       Hybrid encryption in a standard file format: CMS EnvelopedData (RFC 5652), the format behind S/MIME email
├── Hex.java               Small helpers for text and bytes
├── OpenSsl.java           Calls the openssl command-line tool, a different program written by different people, to open a CMS envelope
├── Pem.java               Keys and certificates as PEM: the text format with a "-----BEGIN ...-----" line, Base64 in the middle and an "-----END ...-----" line, which OpenSSL, SSH tools, web servers and most libraries read and write
└── Providers.java         Registers Bouncy Castle as a security provider
```

**Bouncy Castle adds what the JDK leaves out, PEM key files, certificates and the standard CMS envelope, and OpenSSL, a program written by other people, can open what it writes, but the easy calls pick older padding and the library is large.**

The plain version of this lesson used only the JDK, which does the maths but stops there. Real systems exchange keys and encrypted files in **standard
formats** that other programs understand. **Bouncy Castle** is an open-source library that fills the gaps. It reads and writes **PEM**, the text format with a `-----BEGIN ...-----` line that
OpenSSL, SSH and web servers all use. It builds **X.509 certificates**. And it writes **CMS EnvelopedData**, the standard envelope behind S/MIME email, which holds data encrypted with AES, the AES key locked with the
recipient's RSA key, and the recipient named by a certificate. Because it is a standard, a different program can open it, and this project proves that by asking **OpenSSL**, a command-line tool written by other people, to open
the bank's envelope.

The demo keeps the same online bank and customer, Ana, as the plain project. It writes her keys as PEM, protects the private key with a password, makes a self-signed certificate, seals a statement in a CMS envelope, has OpenSSL open it,
and ends with what Bouncy Castle costs.

## The idea in everyday terms

Think of a diplomat's sealed pouch. Any embassy in the world knows the standard form: the sealed bag, the label that names the recipient, the key held by one person. A pouch in a
homemade format could be perfectly secure and still be useless, because nobody else would know how to open it. Bouncy Castle writes the standard pouch, and OpenSSL is the other embassy that proves it can open it.

## The scenario

The online bank and Ana from the plain project. This time the bank must give Ana's keys a form that she can keep in files, and send her statements in a format that her email program and other tools can read. The
demo writes her keys as PEM, makes a certificate for her public key, seals a 10,000-byte statement in a CMS envelope, opens it with Java, with Eve's key, and with OpenSSL, and finally looks at the costs of using the library.

## Run

```bash
./gradlew run
```

The demo tells the story in 5 acts, each printing exact numbers that the tests check:

| Act | What it shows |
| --- | --- |
| 1. Keys as PEM text | A public key and a PKCS#8 private key are written as PEM; the private key in the clear is readable by anyone, encrypted with a password it is not, and a wrong password is refused. |
| 2. A certificate for the public key | A self-signed X.509 certificate names Ana, is valid for the year 2026, is signed with SHA256WITHRSA, and has the same issuer and subject, so anyone could have made it. |
| 3. A standard envelope: CMS EnvelopedData | A 10,000-byte statement is sealed for Ana's certificate with AES-256 and RSA-OAEP; Ana opens it, and Eve's private key is refused. |
| 4. A different program opens it | The openssl command-line tool, written by other people, is given the envelope, the key file and the certificate, and opens the statement: it is a real standard. |
| 5. The bill: what Bouncy Castle costs | The envelope locks its key with RSA-OAEP, but the easy call picks PKCS#1 v1.5; the library is about 7 MB, and registering it changes the whole program. |

## Test

```bash
./gradlew test
```

20 tests in `CertificateTest`, `CmsEnvelopeTest`, `OpenSslInteropTest`, `PemTest`. The unit tests (`./gradlew test`) need nothing installed. The tests that call `openssl`, including the golden-output comparison, are tagged `openssl` and run with `./gradlew integrationTest`, which needs openssl 3 on the PATH.

## What the simulation got right, and what it left out

Keys are fresh and random on every run, so the demo prints only facts that do not depend on them: the headers of the files, the subject and dates of a certificate whose dates are fixed, and whether
operations open or are refused. The certificate is self-signed, with fixed start and end dates so its text does not change. Act four calls the `openssl` command-line tool, which must be version 3 and on the PATH, and says so if it is not. The size
of the library is printed in whole megabytes and belongs to the pinned version.

## Technologies and versions

Every version below is pinned in the project's build files and was the latest stable release when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.bouncycastle:bcprov-jdk18on | 1.86 | Bouncy Castle: algorithms and providers the JDK lacks |
| org.bouncycastle:bcpkix-jdk18on | 1.86 | Bouncy Castle PKIX: PEM, X.509 certificates and CMS |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| OpenSSL (command-line tool) | 3.x | the independent check that opens the CMS envelope; needed for act four and the integration tests |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |

## Learning Material

| Document | What it is for |
| --- | --- |
| [Dependencies](docs/dependencies.md) | what the framework and infrastructure are, and why they are here |
| [Problem statement](docs/problem-statement.md) | the situation and what the project must show |
| [Prerequisites](docs/prerequisites.md) | what you need to know first |
| [Asymmetric Encryption with Bouncy Castle and OpenSSL, explained](docs/asymmetric-encryption-with-bouncy-castle-explained.md) | the acts in prose |
| [Session guide](docs/session.md) | a one-hour lesson with exercises |
| [Animated walkthrough](docs/animation.html) | the acts step by step, narrated |
| [Threat model](docs/threat-model.md) | what is protected, who could harm it, and how, using STRIDE |
| [Spec](docs/spec.md) | what this project must be true of |
| [Expected output](docs/expected-output.txt) | the golden demo output every test and every port checks against |

### The idea in one picture

The statement is encrypted with AES-256; the AES key is locked with the RSA key in Ana's certificate; both go in one standard envelope.

![A CMS envelope for Ana](docs/images/architecture-diagram.png)

### Where each piece sits

Four small classes wrap the library: Pem, Certificates, CmsEnvelope and OpenSsl; Providers registers Bouncy Castle once.

![The classes and what they hold](docs/images/class-diagram.png)

### How the data moves

The demo writes three files; openssl reads them and prints the statement; the demo compares it with the original.

![How the independent check works](docs/images/data-flow-diagram.png)

### Who calls whom, in order



![Sealing and opening an envelope: who calls whom, in order](docs/images/sequence-diagram.png)

### Video

`video/asymmetric-encryption-with-bouncy-castle-explained.mp4` (with `.m4a` audio and `.srt` subtitles) is built by `video/build_video.sh`. Rendered media is not committed.

## What it costs

- **The easy call picks older padding.** Bouncy Castle's simplest way to address an envelope locks the key with PKCS#1 v1.5 padding, which is older and weaker than OAEP. OAEP has to be asked for by name.
- **A large library.** The core jar is several megabytes, and the certificate and CMS code come in a second one. It is a lot to add for a few functions.
- **Registering a provider changes the whole program.** Once Bouncy Castle is added to the JVM's list of providers, every library in the program can be handed algorithms from it.
- **Names and versions move.** The artifacts changed from `jdk15on` to `jdk18on`, and APIs are deprecated over time, so upgrades need care and tests.
- **A self-signed certificate proves nothing.** It binds a name to a key, signed by the key itself, so anyone can make one. Real proof needs a signature from a party that is trusted.
- **AES-CBC has no tag.** This envelope hides the data but does not, on its own, detect changes, which is why newer designs prefer authenticated modes.

## When this is too much

If the plain JDK is enough, because all you need is AES-GCM between two services that you control, use it. Reach for Bouncy Castle when you must
read or write formats the JDK does not know: PEM files, certificates, CMS, PGP, or an algorithm the JDK lacks. The question to ask each time is whether you need the format, or only the maths.

## Where you have already met this

- `-----BEGIN CERTIFICATE-----` and `-----BEGIN PRIVATE KEY-----` in files on a web server.
- `openssl cms -encrypt` and `-decrypt`, and S/MIME encrypted email.
- `id_rsa` and `id_rsa.pub` in an `.ssh` folder, which are PEM-style files.
- Libraries named `bcprov` and `bcpkix` in the dependency lists of Java applications.

## Where this sits

This project is the fifth in [cryptography](..), the library version of [asymmetric-encryption](../asymmetric-encryption).
