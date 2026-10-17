# Patching and Dependency Hygiene with Trivy and Gradle, Explained

## The idea in one sentence

A lock file pins what the build uses, a bill of materials lets a scanner compare it with known problems, a triage turns the report into an order of work and target versions, and checksum verification stops a changed download.

## The 5 acts

### 1. Pinning exactly what the build uses

Gradle's lock file, gradle.lockfile, records the exact version of every library the build resolved: five entries, among them commons-codec 1.23.0, log4j-core 2.14.1 and snakeyaml 1.26. The store asked for four of these. commons-lang3 arrived on its own, as a dependency of commons-text. Without the file, a version range such as 1.+ would pick a different version on a different day.

### 2. A bill of materials a scanner can read

The store writes a CycloneDX 1.6 file listing the five libraries, each with a package URL such as pkg:maven/commons-codec/commons-codec@1.23.0. The file lists the same libraries as the lock file, and the committed copy is exactly what the code writes. A package URL that disagrees with the version beside it is refused, because a scanner would look up the wrong library.

### 3. What the scanner reported

Trivy 0.76.0 scanned the bill of materials, and its report is saved in the project. It reported 16 problems in 4 libraries: 3 critical, 3 high and 10 medium. Every installed version is older than the version that fixes it, as the code compares them. The store's own range checks agree with Trivy on CVE-2021-44228, Log4Shell, and CVE-2022-42889, the Commons Text problem. A scanner only knows what is in its database on the day it runs.

### 4. What to fix first, and to which version

The report is ordered by severity: the Commons Text problem and two log4j-core problems are critical. One move per library fixes everything that has a fix: commons-lang3 to 3.18.0, commons-text to 1.10.0, log4j-core to 2.25.4 and snakeyaml to 2.0. Each target is the highest of the nearest fixes, so log4j-core needs 2.25.4 for its last problem, further than the first fix, 2.15.0, suggested. A fix that crosses a major version, such as snakeyaml 1.26 to 2.0, can break the build, so the tests run after each move.

### 5. Checking what is downloaded, and automating the updates

Gradle's verification-metadata.xml pins the SHA-256 of six downloaded files. One checksum changed in a copy of the file differs from the original, and the integration test shows that the real Gradle refuses to build then. The configuration files for Dependabot and Renovate in the infra folder open a pull request for each update, so each review is small. The cost: every update means reviewing a new hash, and a scanner is only as good as its database.

## The verdict

Lock every build, produce a bill of materials, scan on every build and on a schedule, automate the update pull requests, and verify checksums. Treat a clean scan as today's answer only.

## How to recognise this in code you did not write

- `gradle.lockfile` and `verification-metadata.xml` in repositories.
- A scan step in a CI pipeline that fails the build on a critical finding.
- Dependabot and Renovate pull requests.

## Where you have already met this

- `gradle.lockfile` and `package-lock.json` files in repositories.
- Dependabot and Renovate pull requests with titles such as "Bump log4j-core from 2.14.1 to 2.17.1".
- Security tabs and scan steps in CI pipelines, that run Trivy, Grype or Dependency-Check.
- SBOM requirements in government and customer contracts.
