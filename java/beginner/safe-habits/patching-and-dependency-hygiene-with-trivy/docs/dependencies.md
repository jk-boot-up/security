# Dependencies

This project adds no Java library. It uses three tools: Docker to run Trivy, Gradle's own locking and verification features, and the CycloneDX format.

## What Trivy is

an open-source vulnerability scanner from Aqua Security. It reads a bill of materials or a lock file and compares the libraries with a database of known problems. It runs here from the Docker image aquasec/trivy:0.76.0.

## What Docker is

runs the Trivy image, so that nothing needs installing besides Docker. A named volume keeps the vulnerability database between runs.

## What CycloneDX 1.6 is

an open standard for a bill of materials, written here as JSON by about eighty lines of Java.

## What Gradle dependency locking and verification is

built into Gradle. `gradle.lockfile` pins versions, and `gradle/verification-metadata.xml` pins SHA-256 checksums. The fixture project in `fixtures/verified-build` has a real one that Gradle generated.

## Why this project uses them

The plain project worked out by hand what the tools do. A scanner is what teams actually use, and writing a reader for its report shows what it does and does not tell you.

## What to install

A JDK for the demo and the ordinary tests. For the integration tests, Docker and, once, a network connection to download Trivy's database. The versions:

| Tool | Version |
| --- | --- |
| Trivy | 0.76.0 |
| CycloneDX specification | 1.6 |
| Gradle | 9.8.1 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The integration tests need Docker and download about 130 MB the first time.
- A report depends on the day's database, so exact counts are not asserted by the integration tests.
