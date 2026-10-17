# Problem Statement

## The scenario

An online store depends on libraries written by other people. Some of them have problems that become known after the store ships. The store needs to find out which, and quickly.

## The naive version

Upgrade when something breaks, or when somebody remembers. Libraries drift and nobody can say which version is running.

## What this project must deliver

- A lock file read by the code, so the exact libraries are known.
- A CycloneDX 1.6 bill of materials written and checked by the code.
- A report from the real Trivy, read and cross-checked with the code's own range checks.
- A triage that orders the problems and names one target version per library.
- Gradle's dependency verification, with a test that a changed checksum stops the build.
- Dependabot and Renovate configuration files, as documentation.
