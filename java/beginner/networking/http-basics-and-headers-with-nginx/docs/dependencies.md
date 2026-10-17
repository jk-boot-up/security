# Dependencies

This project needs Docker, and uses one container image. The Java code is the plain JDK only.

## What Docker is

a tool that runs programs in isolated containers. The demo starts one container for each configuration and stops it afterwards. Docker must be installed and running.

## What nginx is

a very widely used web server and reverse proxy. A reverse proxy sits in front of an application and passes requests to it, which makes it the natural place to set security headers and limits.

## Why this project uses them

The plain project models HTTP in Java. Headers and limits live in a real web server, whose directives have rules, such as always and inheritance, that a model can only describe. Running the real nginx lets the demo show the mistakes that are made in real configurations.

## What to install

A JDK and a running Docker. Gradle downloads the Java side, Docker pulls the nginx image the first time, and the versions are pinned:

| Tool | Version |
| --- | --- |
| nginx image | nginx:1.31.7-alpine |
| Docker | any recent Docker Engine or Docker Desktop |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The demo and the tagged tests need Docker; the default `./gradlew test` does not.
- A container takes a second or two to start, so the demo is slower than the plain version.
