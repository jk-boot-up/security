# Dependencies

This project needs Docker, and uses one container image. The Java code is the plain JDK only.

## What Docker is

a tool that runs programs in isolated containers. The demo starts one container for each act and stops it afterwards. Docker must be installed and running.

## What nginx is

a very widely used web server and reverse proxy. A reverse proxy sits in front of an application and passes requests to it, which makes it a natural place for a rate limit. `limit_req` is nginx's rate limit.

## Why this project uses them

The plain project models a login's defences in Java, with a clock made of ticks. A real limit lives in a real piece of software, uses real time, and has choices, such as the key, that the model could only describe. Running the real nginx lets the demo show the mistake that matters most: a key the attacker can choose.

## What to install

A JDK and a running Docker. Gradle downloads the Java side, Docker pulls the nginx image the first time, and the versions are pinned:

| Tool | Version |
| --- | --- |
| nginx image | nginx:1.31.6-alpine |
| Docker | any recent Docker Engine or Docker Desktop |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The demo and the tagged tests need Docker; the default `./gradlew test` does not.
- A container takes a second or two to start, so the demo is slower than the plain version.
- Real-time rates make exact counts depend on timing, which is why the demo uses a very slow rate.
