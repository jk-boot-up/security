# Dependencies

This project uses Spring Boot, which brings the web server and the security filters with it.

## What Spring Boot is

starts an application with a web server inside it and chooses sensible settings from what is on the classpath.

## What Spring Security is

puts a chain of filters in front of every request and offers method security, a role hierarchy and the AuthorizationManager interface that the custom rules implement.

## What Spring MVC is

maps a web address to a Java method. The store's endpoints are plain controller methods.

## What Apache Tomcat (embedded) is

the web server inside the application.

## Why this project uses them

The plain project models the rules so that they are visible. This one shows how a framework expresses the same rules in a few lines, and how its own authorization manager gives the numbers for what a stolen account can reach.

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
- The demo uses plain HTTP and basic credentials on the loopback interface; real HTTPS and token login are other projects.
- The grant store and the accounts are in memory, so a restart forgets them. A database table is the production shape.
