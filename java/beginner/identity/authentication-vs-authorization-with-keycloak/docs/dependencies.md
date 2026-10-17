# Dependencies

This project uses Spring Boot's resource server support, and Keycloak as the login server in the integration tests. The demo needs no container.

## What Spring Boot is

builds the order API and starts it on a free port. Boot 4.1.1 is the latest stable release, and its dependency list pins the Spring versions below.

## What Spring Security (OAuth 2 resource server) is

checks the bearer token, builds the authentication from its claims and answers 401 and 403.

## What Nimbus JOSE + JWT is

reads and verifies JWTs, and signs the stand-in tokens in the demo.

## What Keycloak is

the open-source login server. The integration tests run it in Docker with the shop realm from `infra/shop-realm.json`.

## What Docker is

runs the Keycloak container for the integration tests; the demo does not need it.

## Why this project uses them

The plain and Spring Security projects kept users inside the shop. This project shows the design most companies actually use: the shop trusts a login server and checks what it is given.

## What to install

A JDK for the demo and the default tests. Docker, for the integration tests. Gradle downloads the Java libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.1 |
| Nimbus JOSE + JWT | 10.9.1 |
| Tomcat (embedded) | 11.0.24 |
| Keycloak (Docker image) | 26.8.1 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- About 40 MB of Spring Boot and Tomcat libraries for a small API.
- The integration tests download and start Keycloak, which takes about twenty seconds.
- The Keycloak image is large (about 500 MB) and is pulled once.
