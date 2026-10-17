# Dependencies

This project uses Spring Boot, which brings Spring Framework, Spring MVC on Tomcat, and Spring Security.

## What Spring Boot is

starts the store as a real web server on a free loopback port and wires its parts together. Its bill of materials pins every Spring version.

## What Spring Security is

the filter chain (`authorizeHttpRequests`), HTTP Basic sign-in, method security (`@PreAuthorize`, `@PostAuthorize`) and the `AuthorizationDeniedEvent` the denial log listens to.

## What Spring MVC is

the controllers, and the `RequestMappingHandlerMapping` that the route inventory reads to list every route really served.

## Why this project uses them

The plain project showed access control built by hand. Most Java services use Spring Security, and most access-control bugs in them are configuration mistakes the framework enforces faithfully.
This project shows those mistakes and the framework's own tools for preventing them.

## What to install

A JDK. Gradle downloads the libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.1 |
| Tomcat (embedded) | 11.0.24 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- A framework of tens of megabytes for an API of eleven routes.
- Rules split between the filter chain and annotations, which must both be read.
- Each run starts real servers, so the demo and tests take seconds rather than milliseconds.
