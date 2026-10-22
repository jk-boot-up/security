# Prerequisites

## Required

- Java basics
- The plain sql-injection project, for the idea of injection
- Docker running locally, for the demo and the integration tests

## Explicitly not required

- Spring: Hibernate is used directly
- A PostgreSQL installation: the demo starts and removes its own

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- Docker, with the image `postgres:18.6-alpine` (pulled on first use)
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.hibernate.orm:hibernate-core | set by the build | library the code uses |
| org.postgresql:postgresql | set by the build | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
