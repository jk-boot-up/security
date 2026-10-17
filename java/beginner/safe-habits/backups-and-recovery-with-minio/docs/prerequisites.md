# Prerequisites

## Required

- Java basics
- The backups-and-recovery project, or an idea of what a backup and a restore test are
- Docker running on your machine

## Explicitly not required

- An AWS account: everything runs locally
- Knowledge of S3 or MinIO

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- Docker (the demo and the integration tests pull the two images on first use)
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| software.amazon.awssdk:s3 | set by the build | library the code uses |
| software.amazon.awssdk:url-connection-client | set by the build | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| RustFS (rustfs/rustfs) | 1.0.2 | the S3-compatible object store that runs in Docker; MinIO's community edition no longer publishes images, and RustFS speaks the same S3 and MinIO-style admin API |
| rc (rustfs/rc) | v0.1.36 | the admin command-line tool that creates the backup user and its policy; the same job `mc` does for MinIO |
| Docker | 24 or later | runs the store and the admin tool; only the integration tests and the demo need it |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
