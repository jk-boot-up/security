# Session Guide — Patching and Dependency Hygiene with Trivy and Gradle

## Learning Objectives

By the end of the session you can:

- explain what a lock file and a bill of materials are for
- read a scanner's report and order its findings
- choose the version to move a library to
- say what dependency verification protects against
- say what a scanner cannot tell you

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Pinning exactly what the build uses | 7 min |
| 0:17 | Act 2: A bill of materials a scanner can read | 7 min |
| 0:24 | Act 3: What the scanner reported | 7 min |
| 0:31 | Act 4: What to fix first, and to which version | 7 min |
| 0:38 | Act 5: Checking what is downloaded, and automating the updates | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the ingredients list. Run the demo, then open the Trivy report in the fixtures folder and match a line to the triage. Ask what would change if the scan ran next month, and what a clean scan would prove.

## Exercises

1. Run Trivy yourself with `docker run aquasec/trivy:0.76.0 sbom fixtures/online-store.cdx.json` and compare the report with the saved one.
2. Add a library to the lock file and regenerate the bill of materials.
3. Run `./gradlew --write-verification-metadata sha256 resolveAll` in the fixture project after changing its version.
4. Apply the upgrade targets and scan again.
