# Prerequisites

## Required

- Java basics
- The tcp-ip-basics project, or an idea of what a SYN, ACK and sequence number are

## Explicitly not required

- Root access: only recorded files are read
- Wireshark, though it opens the same file

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- tcpdump and Python 3 for the integration tests only
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| tcpdump | 4.99.1 (libpcap 1.10.1) | the independent check that prints the capture the Java code writes; needed for the integration tests |
| Python | 3.14 | runs the independent flood detector in python/; standard library only |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
