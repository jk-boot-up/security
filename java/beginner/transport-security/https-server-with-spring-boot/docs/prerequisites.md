# Prerequisites

## Required

- Java basics
- The tls-handshake and https-server projects, or an idea of what a certificate and a handshake are

## Explicitly not required

- Any Spring experience beyond reading a few properties
- A real domain name or certificate authority: the demo uses a hosts file and demo certificates

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1, set by spring-boot-dependencies | library the code uses |
| org.springframework.boot:spring-boot-starter-security | 4.1.1, set by spring-boot-dependencies | library the code uses |
| org.springframework.boot:spring-boot-starter-webmvc-test | 4.1.1, set by spring-boot-dependencies | library the code uses (tests only) |
| org.springframework.boot:spring-boot-starter-security-test | 4.1.1, set by spring-boot-dependencies | library the code uses (tests only) |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| Spring Boot | 4.1.1 | the latest stable release (the 4.2 line is only milestones); it starts the web server and reads the SSL bundles |
| Spring Framework (Spring MVC, RestClient) | 7.0.9 | maps web addresses to Java methods, and its RestClient is the client in the demo; pinned by Spring Boot 4.1.1 |
| Spring Security | 7.1.1 | redirects plain HTTP to HTTPS and writes the HSTS header; pinned by Spring Boot 4.1.1 |
| Apache Tomcat (embedded) | 11.0.24 | the web server inside the application; it does the TLS handshake; pinned by Spring Boot 4.1.1 |
| keytool (JDK) | 25 | made the demo keystores once; the script is in infra/ |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
