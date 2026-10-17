# Prerequisites

## Required

- Java basics
- The plain totp-multi-factor project, or at least what a one-time code is

## Explicitly not required

- Writing HMAC yourself: the library does it
- A QR code library: only the text in the QR code is shown
- A phone: the demo needs none

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| com.eatthepath:java-otp | 1.0.0 | java-otp: HOTP and TOTP one-time codes |
| commons-codec:commons-codec | 1.23.0 | Apache Commons Codec: Base32 and other encoders |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
