# SBOMs and Scanning with syft and grype, Explained

## The idea in one sentence

You cannot answer whether you are affected by an advisory without a list of what you ship, and the list is useful only if it is written at every build, scanned again on a schedule and read by a gate that has rules for exceptions.

## The 5 acts

### 1. What a bill of materials answers, and what it does not

The input is the image shop-sbom:2.14.1: the base image eclipse-temurin:21-jre-alpine with log4j-core-2.14.1.jar and log4j-api-2.14.1.jar copied into /app/lib, and
the command syft shop-sbom:2.14.1 -o cyclonedx-json. The result is a CycloneDX bill whose subject is shop-sbom:2.14.1, listing the operating system packages as
pkg:apk/... and the libraries log4j-api@2.14.1 and log4j-core@2.14.1.
One package URL part by part: pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1 is the type maven, the namespace org.apache.logging.log4j, the name log4j-core and
the version 2.14.1. Its fingerprint: the SHA-1 the bill records equals the SHA-1 this program works out from the jar, and the Apache-2.0 licence is named. The
repository, the commit and the builder are not in the bill: that is provenance.

### 2. Scanning it against known advisories

The input is the command grype shop-sbom:2.14.1 -o json and grype's advisory database, built within the last seven days. The result: in log4j, the critical
CVE-2021-44228 (log4shell, fixed in 2.15.0) and CVE-2021-45046 (fixed in 2.16.0), and the high CVE-2021-45105 (fixed in 2.17.0).
How one match is made: the bill says log4j-core 2.14.1, the advisory CVE-2021-44228 is fixed in 2.15.0, and 2.14.1 is below 2.15.0, so it matches.
The same scan from the bill alone (grype sbom:sbom.cdx.json) finds the same advisories in log4j; the rest of the findings are in the base image's operating system
packages, and their number changes daily, so it is not printed.

### 3. Fixing it, and checking that it is fixed

The input: the fixes needed are 2.15.0, 2.16.0 and 2.17.0, so the upgrade must be to at least 2.17.0, and the team builds shop-sbom:2.25.5.
The result: comparing the two bills, log4j-api@2.14.1 and log4j-core@2.14.1 are removed and log4j-api@2.25.5 and log4j-core@2.25.5 added; the operating system
packages are the same in both. The new scan finds no critical or high advisory in log4j: 3 before, 0 after.

### 4. A gate that reads the scan

The input is the rule (refuse a critical or high advisory in the store's libraries; a written exception may cover a high one, until its end day) and one exception:
CVE-2021-45105 (high), the reason, ending on day 120.
The result for six requests: shop-sbom:2.14.1 is refused with 2 critical and 1 high; with the exception on day 100 it is still refused with 2 critical, because an
exception cannot cover a critical advisory. shop-sbom:2.16.0 is refused with 1 high; with the exception on day 100 it is deployed with 1 high excepted; on day 130 the
exception has run out and it is refused again. shop-sbom:2.25.5 is deployed.

### 5. The bill

The input is an image whose /app/lib holds the log4j-core-2.14.1.jar renamed blob.dat. The result: syft lists no log4j library and grype finds no advisory, because a
scanner sees only what the cataloguer recognises.
The bill written for shop-sbom:2.14.1, offered for shop-sbom:2.25.5, does not describe it: write the bill at every build. And a scan cannot see tomorrow's advisories
(keep the bills and rescan on a schedule), cannot know whether the store calls the vulnerable code (a VEX statement records that), and cannot say who built the image
(signed provenance, in the plain sbom-and-provenance project).

## The verdict

Write a bill of materials for every image at build time, keep it with the image, scan it on a schedule with a fresh database, gate on critical and high advisories with exceptions that name a reason and an end date, and know what your cataloguer cannot see.

## How to recognise this in code you did not write

- `syft ... -o cyclonedx-json` or `grype sbom:...` in a pipeline.
- A `bom.json` attached to a release.
- A package URL such as `pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1`.
- A scanner with thousands of findings in the base image.

## Where you have already met this

- `syft` or `grype` as a step in a pipeline.
- A `bom.json` or `sbom.cdx.json` attached to a release.
- A scanner report with hundreds of findings, most of them in the base image.
- A `.grype.yaml` file with ignore rules.
