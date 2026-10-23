# Prerequisites

## Required

- Java basics
- Docker running
- The images eclipse-temurin:25-jre (pulled on the first run) and the SPIRE 1.15.5 release (downloaded once into build/spire, about 55 MB)
- Understanding of TLS and client certificates (see mutual-tls-between-services)

## Explicitly not required

- A cloud account
- Kubernetes
- A macOS SPIRE build (there is none)

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- docker
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| io.spiffe:java-spiffe-provider | 0.8.17 | library the code uses |
| io.grpc:grpc-netty-shaded | 1.80.0 | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
