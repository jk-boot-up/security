# Prerequisites

## Required

- Java basics: classes, interfaces, maps
- The plain password-hashing project, or at least the ideas of a hash, a salt and a slow hash

## Explicitly not required

- Spring Boot, a web server or a database
- The mathematics of bcrypt or Argon2
- Knowing how Spring Security's login filters work: this project uses only the password encoders

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.springframework.security:spring-security-crypto | 7.1.1, set by spring-security-bom | password encoders: bcrypt, PBKDF2, scrypt, Argon2 |
| org.bouncycastle:bcprov-jdk18on | 1.86 | Bouncy Castle: algorithms and providers the JDK lacks |
| org.springframework:spring-core | 7.0.9, set by spring-framework-bom | Spring's utility classes and logging, which the crypto module expects the application to supply |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
