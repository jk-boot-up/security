# Patching and Dependency Hygiene with Trivy and Gradle, Explained

## The idea in one sentence

A lock file pins what the build uses, a bill of materials lets a scanner compare it with known problems, a triage turns the report into an order of work and target versions, and checksum verification stops a changed download.

## The 5 acts

### 1. Pinning exactly what the build uses

Gradle's lock file, gradle.lockfile, records the exact version of every library the build uses: 5 entries, each a group, an artifact and a version, such as org.yaml snakeyaml 1.26.
The store's build asked for 4 of the 5. commons-lang3 came on its own, as a dependency of commons-text. Before the lock, a range such as 1.+ means the newest 1.x on the day of the build, so two builds can differ. After the lock, the same 5 versions are used on every build.

### 2. A bill of materials a scanner can read

The store turns its lock file into a software bill of materials in the CycloneDX 1.6 format: 5 components. The input is one line, org.yaml:snakeyaml:1.26; the output is a component with a package URL, pkg:maven/org.yaml/snakeyaml@1.26.
The package URL, or purl, is read part by part: the type pkg:maven, the group org.yaml, the name snakeyaml, and the version 1.26. It is the name a scanner looks up.
Three checks: the SBOM lists the same 5 libraries as the lock file; the committed fixtures/online-store.cdx.json is exactly what the code writes; and a component whose version says 1.31 while its purl says 1.26 is refused.

### 3. What the scanner reported

Trivy 0.76.0 scanned online-store.cdx.json, and its report is saved in fixtures/trivy-report.json, so the demo is the same on every run. It reported 16 problems in 4 libraries: 3 critical, 3 high, 10 medium and 0 low; log4j-core and snakeyaml have 7 each, commons-lang3 and commons-text 1 each.
The store's own code cross-checks the report: every installed version is older than its fix, and for CVE-2021-44228 and CVE-2022-42889 the store's own range check and Trivy agree.
A scanner only knows what is in its database on the day it runs, so a clean report means no known problem, on that day.

### 4. What to fix first, and to which version

The findings are ordered by severity: the five most serious are three critical and two high, in commons-text, log4j-core and snakeyaml.
For each library the target is the highest of its nearest fixes. log4j-core 2.14.1 has fixes 2.15.0, 2.16.0, 2.17.0, 2.17.1, 2.25.3 and 2.25.4: moving to the first fix, 2.15.0, would leave six problems; 2.25.4 fixes all 7 at once.
So there is one move per library: commons-lang3 3.11 to 3.18.0, commons-text 1.9 to 1.10.0, log4j-core 2.14.1 to 2.25.4 and snakeyaml 1.26 to 2.0. The last one crosses a major version, which can break the build, so run the tests after each move.

### 5. Checking what is downloaded, and automating the updates

Gradle's verification file, gradle/verification-metadata.xml, pins the SHA-256 of 6 downloaded files: the commons-codec jar and pom, and four parent and bill-of-materials poms.
Before the check, a copy of the file has one checksum changed, for commons-codec-1.23.0.jar. After the check, 1 file differs, and Gradle refuses to build (the integration test runs this with the real Gradle).
dependabot.yml and renovate.json in infra/ open one pull request for each update, so each review is small. The cost: every new hash needs a review, and a scanner knows only what is in its database.

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
