# Prerequisites

## Required

- Java basics
- The patching-and-dependency-hygiene project, or an idea of what a known vulnerability and a version range are

## Explicitly not required

- Docker, for the demo and the ordinary tests
- An account with any scanning service

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- Docker, for the integration tests only
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| Trivy | 0.76.0 (Docker image aquasec/trivy:0.76.0) | the scanner that reads the bill of materials and the lock file; used by the integration tests |
| Docker | any recent version | runs Trivy without installing it |
| CycloneDX | 1.6 (JSON) | the standard format of the bill of materials |
| Gradle dependency locking and verification | Gradle 9.8.1 | gradle.lockfile pins versions; verification-metadata.xml pins SHA-256 checksums |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
