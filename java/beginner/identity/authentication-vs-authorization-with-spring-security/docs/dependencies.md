# Dependencies

This project uses three pieces of Spring. None of them needs a web server or a database to run here.

## What Spring Security is

a library that puts a chain of security filters in front of a web application. One filter reads the login, another checks that the request is allowed, and a third turns a refusal into a 401 or a 403. It also provides password encoders such as BCrypt, and method security with `@PreAuthorize`.

## What Spring MVC is

the part of Spring that maps a web address, such as /orders/O-1, to a Java method. The shop's endpoints are ordinary methods on a controller.

## What MockMvc is

a test tool that sends a request through the whole filter chain and the controller in memory, without opening a network port. The demo uses it so every run is fast and repeatable.

## Why this project uses them

The plain project builds the login, the sessions and the rules by hand, so the ideas are visible. This project shows how the same ideas are applied with a framework, which is how most real applications do it. Putting the two side by side shows what the framework does for you, and what it still leaves to you: the rules, the configuration, and remembering to protect every endpoint.

## What to install

Only a JDK. Gradle downloads the rest, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Spring Framework (spring-webmvc, spring-test) | 7.0.9 |
| Spring Security (web, config, crypto) | 7.1.1 |
| Jakarta Servlet API | 6.1.0 |
| JUnit | 5.10.2 |

## What it costs

- A framework is a large dependency, and the filter configuration is code that has to be read as carefully as the rules.
- Method security needs the compiler flag that keeps parameter names, or `#id` in a rule fails at run time.
- The demo is a simulation of a web server: a production application also needs HTTPS, CSRF protection and a real user store.
