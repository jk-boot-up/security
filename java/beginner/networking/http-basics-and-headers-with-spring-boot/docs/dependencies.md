# Dependencies

This project uses Spring Boot, which brings the web server and the security filters with it.

## What Spring Boot is

starts an application with a web server inside it and chooses sensible settings from what is on the classpath. Properties such as `server.servlet.session.cookie.same-site` change them.

## What Spring Security is

puts a chain of filters in front of every request. One of them writes the security headers, and the configuration decides which.

## What Spring MVC is

maps a web address to a Java method. The store's endpoints are plain controller methods.

## What Apache Tomcat (embedded) is

the web server inside the application. It parses requests, enforces the header size limit and refuses a line break in a header.

## What MockMvc is

a test tool that sends a request through the whole filter chain in memory, without a network port.

## Why this project uses them

The plain project builds HTTP by hand, so the rules are visible. This one shows how a framework applies the same rules with configuration, and how a real server behaves when attacked, which is what most applications run on.

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
- Defaults change between releases, so the demo's scores are a check to rerun after an upgrade.
- The demo uses plain HTTP on the loopback interface; real HTTPS is the next module.
