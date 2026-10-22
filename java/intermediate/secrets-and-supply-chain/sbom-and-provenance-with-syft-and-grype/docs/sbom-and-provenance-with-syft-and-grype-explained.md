# SBOMs and Scanning with syft and grype, Explained

## The idea in one sentence

You cannot answer whether you are affected by an advisory without a list of what you ship, and the list is useful only if it is written at every build, scanned again on a schedule and read by a gate that has rules for exceptions.

## The 5 acts

### 1. What a bill of materials answers

syft catalogues the image shop-sbom:2.14.1 without running it and writes a CycloneDX JSON document. It lists operating system packages and the application's libraries, log4j-core and log4j-api 2.14.1, each by its package URL, which names the ecosystem, the library and the version. Hashes and licences are there. What it does not say is which repository, commit or builder made the image; that is what provenance is for.

### 2. Scanning it against known advisories

grype matches the image's packages to its vulnerability database: two critical advisories in log4j, CVE-2021-44228 and CVE-2021-45046, and one high, CVE-2021-45105, with the versions that fix them. Scanning the bill alone, not the image, finds the same advisories. The rest of the findings come from the base image's operating system packages, and their number changes every day, so the demo does not print it.

### 3. Fixing it, and checking that it is fixed

The upgrade that clears every critical and high advisory is 2.17.0 or later, and the team builds 2.25.5. The new bill is compared with the old: it removes the 2.14.1 libraries and adds the 2.25.5 ones, and the base image's packages are the same in both. The scan of the new image has no critical or high advisory in log4j, where the old image had three.

### 4. A gate that reads the scan

The gate refuses an image with a critical or high advisory in the store's libraries. The old image is refused with two critical and one high. An exception for the high one does not help, because an exception cannot cover a critical advisory. 2.16.0 fixes the critical ones and is still refused because of the high one. With a written exception that names the advisory, the reason and the day it ends, it is deployed on day 100 and refused on day 130, after the exception ran out on day 120. 2.25.5 is deployed.

### 5. The bill

The same log4j-core 2.14.1 jar renamed blob.dat in another image is in no bill and in no scan: a scanner finds only what the cataloguer recognises. The bill written for 2.14.1 does not describe 2.25.5, so a bill must be written at every build. The database the scan matches against is data, with an age. An advisory published tomorrow matches a bill written today, so the bills must be kept and scanned again on a schedule. A finding is a question, not a verdict: a vulnerable library the store never calls is still reported, and a statement of exploitability records the answer.

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
