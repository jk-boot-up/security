# Dependencies

This project uses Spring Boot with Spring MVC, Spring Security and Thymeleaf. The versions come from the Spring Boot BOM.

## What Spring Boot is

starts the store as a real web server, with Tomcat, on a free port of the loopback interface.

## What Spring Security is

writes the response headers, including the Content-Security-Policy that this store fills with a fresh nonce.

## What Thymeleaf is

the template engine. It escapes values written with th:text for their place in the page, and writes raw markup only with th:utext.

## Why this project uses them

The plain project wrote an encoder for each place by hand, to show what encoding is. Real applications use a template engine that encodes by default, so the lesson becomes knowing what the
engine does for each place, and where it stops.

## What to install

A JDK. Gradle downloads the libraries, and the versions are pinned through the Spring Boot BOM:

| Tool | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.1 |
| Thymeleaf | 3.1 (Boot BOM) |
| Tomcat | 11.0.24 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- A full web framework for one page.
- A per-request nonce needs a filter of the store's own; Spring Security's policy setting is a fixed string.
