# Prerequisites

## Required

- Java basics
- The least-privilege project, or an idea of roles and deny by default

## Explicitly not required

- A cloud account: everything runs locally
- Terraform experience: the code is short and commented

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- OpenTofu or Terraform, and Docker, for the integration tests only
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| tools.jackson.core:jackson-databind | 3.2.3 | library the code uses |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| OpenTofu | 1.13.1 | applies the Terraform code in the integration tests; Terraform itself also works and is used if OpenTofu is missing |
| Terraform AWS provider | 6.69.1 | talks to the emulator's IAM service |
| LocalStack (community image) | 4.14.0 | a local AWS emulator in Docker; the last community tag that starts without an account token |
| Docker | required for the integration tests | runs the emulator |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
