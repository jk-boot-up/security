# Prerequisites

## Required

- Java basics: classes, interfaces, annotations and a little streams
- The plain authentication-vs-authorization project, or at least the idea of 401 and 403
- A first idea of what a Spring bean is

## Explicitly not required

- Spring Boot: this project uses Spring Framework and Spring Security directly
- A database, a login page or a browser
- Knowing BCrypt internals: it is used as a black box and explained in the password-hashing project

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- About an hour for the session guide.

## Tech stack and versions

Pinned in the project's build files, and the latest stable releases when the project was built.

| Technology | Version | Used for |
| --- | --- | --- |
| Java | 25 | the language and runtime; a Gradle toolchain finds or downloads the JDK |
| Gradle | 9.8.1 (wrapper) | build and run; nothing to install, the wrapper downloads it |
| org.springframework:spring-webmvc | 7.0.9, set by spring-framework-bom | Spring MVC: maps web addresses to controller methods |
| org.springframework:spring-test | 7.0.9, set by spring-framework-bom | MockMvc, which sends requests through the real filter chain without a network port |
| org.springframework.security:spring-security-web | 7.1.1, set by spring-security-bom | Spring Security's filter chain |
| org.springframework.security:spring-security-config | 7.1.1, set by spring-security-bom | Spring Security's configuration, `@EnableMethodSecurity` and `HttpSecurity` |
| jakarta.servlet:jakarta.servlet-api | 6.1.0 | the servlet API that Spring MVC and Spring Security are written against |
| org.junit.jupiter:junit-jupiter | 6.1.3, set by junit-bom | JUnit 5: the tests (tests only) |
| org.junit.platform:junit-platform-launcher | 6.1.3, set by junit-bom | JUnit platform launcher, which Gradle needs to run the tests (tests only) |
| videokit | repository tool (`tools/videokit`) | the narrated video and animation: Piper `en_US-amy-medium`, speed 0.8, longer pauses (the amy-slow voice) |
