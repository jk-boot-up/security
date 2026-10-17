# Dependencies

This project uses Spring Boot, which brings the web server and the security filters with it, and keytool from the JDK, which made the demo keystores.

## What Spring Boot is

starts an application with a web server inside it, reads SSL bundles from properties, and watches their files so a renewed certificate is used without a restart.

## What Spring Security is

puts a chain of filters in front of every request. One redirects plain HTTP to HTTPS, and another writes the HSTS header.

## What Spring MVC and RestClient is

map web addresses to Java methods, and make the client calls in the demo, here on the JDK's HttpClient, which verifies host names.

## What Apache Tomcat (embedded) is

the web server inside the application. It does the TLS handshake and, with client-auth need, checks client certificates.

## What keytool is

the JDK tool that made the keystores, once. The script is infra/make-test-keystores.sh and the password is a demo value.

## Why this project uses them

The plain project shows each piece by hand. This one shows how a framework turns the same decisions into configuration, and what it leaves for you to check.

## What to install

Only a JDK. Gradle downloads the rest. Spring Boot 4.1.1 is the latest stable release (the 4.2 line is only milestones), and its dependency list pins everything else:

| Tool | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.1 |
| Apache Tomcat (embedded) | 11.0.24 |
| Jakarta Servlet API | 6.1.0 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- A large dependency, and the versions must move together when a vulnerability is fixed.
- Defaults change between releases, so the demo is a check to rerun after an upgrade.
- The demo uses a hosts file and demo certificates, which a real deployment replaces with a public name and a real certificate authority.
