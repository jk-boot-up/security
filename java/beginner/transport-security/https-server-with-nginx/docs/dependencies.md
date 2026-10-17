# Dependencies

This project adds no Java library. It runs one container image and uses Docker to do it. OpenSSL was used once, to make the demo certificates, and is not needed to run the project.

## What nginx (the image nginx:1.31.7-alpine) is

the web server and reverse proxy in front of the store. The image is pinned to an exact version, so every run uses the same nginx.

## What Docker is

runs the container for the demo and for the tagged tests. Containers are started with --rm and always stopped at the end.

## What OpenSSL 3.6.3 (script only) is

`infra/make-test-pki.sh` used it once to make the demo root, the shop certificate, an impostor and Ana's client certificate. The files are committed; the root's private key was thrown away.

## Why this project uses them

TLS is usually ended in front of the application, so the lesson is easier to trust when the real front door is used and the Java client checks it the way a browser would. The Java side uses only the JDK.

## What to install

A JDK and a running Docker. Gradle downloads the Java side, Docker pulls the nginx image the first time, and the versions are pinned:

| Tool | Version |
| --- | --- |
| nginx image | nginx:1.31.7-alpine |
| Docker | any recent Docker Engine or Docker Desktop |
| OpenSSL (certificates, script only) | 3.6.3 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The demo and the tagged tests need Docker; the default `./gradlew test` does not.
- A container takes a second or two to start, so the demo is slower than the plain version.
- The demo certificates and keys are committed on purpose and protect nothing.
