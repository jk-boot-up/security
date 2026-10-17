# Beginner: Foundations (Java)

One self-contained project per concept, in Java 25 with Gradle. Each project has runnable code
with deterministic tests, a README, diagrams, a step-by-step animation, a session guide and a
narrated video. Every project runs on your own machine, with no cloud account.

Read the modules in order:

| Module | Covers |
| --- | --- |
| [foundations](foundations) | the CIA triad, threats and risk |
| identity | authentication and authorization |
| passwords-and-mfa | password hashing, password attacks, one-time codes |
| cryptography | hashing, encryption, signatures, keystores |
| networking | TCP/IP, DNS, HTTP |
| transport-security | the TLS handshake, certificates, HTTPS |
| safe-habits | least privilege, patching, backups |

Only `foundations` exists so far. The full plan is in [`../../docs/beginner-plan.md`](../../docs/beginner-plan.md).

## Run a project

```bash
cd foundations/cia-triad
./gradlew test
./gradlew run
```

Gradle finds or downloads JDK 25 by itself. If Gradle cannot find a JDK 25 you installed, point it at one with
`-Dorg.gradle.java.installations.paths=/path/to/jdk-25`.

## Build a project's docs, video and animation

```bash
tools/conceptkit/conceptkit.sh build cia-triad
```

This reruns the tests and regenerates the docs, diagrams, thumbnail, video, animation narration and
the HTML pages from the project's `concept.toml`. Videos and audio are build output and are not committed.
It needs Python 3, `ffmpeg` and Node. Nothing is uploaded.
