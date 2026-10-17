# Prerequisites

## Required

- Java basics
- The dns-basics project, or an idea of what a DNS answer and a time to live are
- Docker running on your machine

## Explicitly not required

- Running a production DNS server
- Go, or any knowledge of how CoreDNS is built

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- Docker (Docker Desktop or Docker Engine)
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| CoreDNS (container image) | coredns/coredns:1.14.7 | the real DNS server the Java code asks, pinned to an exact version |
| Docker | any recent Docker Engine or Docker Desktop | runs the CoreDNS containers for the demo and the container tests |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
