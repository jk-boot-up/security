# Patching and Dependency Hygiene, Explained

## The idea in one sentence

Most of a program is other people's code, so a team must list what it ships, match it against known problems, fix the reachable and severe ones first within deadlines, and check that every download matches a hash it reviewed.

## The 5 acts

### 1. An inventory of what the store ships

The store's software bill of materials, sbom.json, lists 7 components: the store itself, shop-app, and 6 libraries. The demo prints it as a tree, so each library sits under the one that needs it.
Three are direct, chosen by the team in its build file: acme-http, acme-image and acme-log. Three are transitive, and came on their own as dependencies of dependencies: acme-json, acme-crypto and acme-xml. Nobody on the team chose them, and a problem in any of them is still the store's problem.

### 2. Matching the inventory against known problems

The advisory file lists 5 known problems, each with the library, the affected range and the fixed version. They are fictional, made up for this lesson.
Versions must be compared as numbers. As text, 1.10.0 comes before 1.9.0, because the character 1 comes before 9; as numbers, 10 is more than 9, so 1.10.0 is newer. The range >=1.5.0 <1.8.3 means from 1.5.0, up to but not including 1.8.3: so 1.8.2 is in it and 1.8.3, the fixed version, is not.
Three of the five apply to this store. DEMO-2026-0001 in acme-json and DEMO-2026-0002 in acme-xml are transitive: force the fixed version, then ask the library above it for a release that needs it. DEMO-2026-0003 in acme-log is direct: change the version. The other two are already at their fixed versions.

### 3. How bad, and what to fix first

A CVSS vector describes a flaw in eight short metrics. For DEMO-2026-0001 it is AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H: over the network, low complexity, no privileges, no user action, scope unchanged, and high loss of confidentiality, integrity and availability.
The score is worked out in five steps. ISS = 1 - (1-0.56)(1-0.56)(1-0.56) = 0.915. Impact = 6.42 x 0.915 = 5.873. Exploitability = 8.22 x 0.85 x 0.77 x 0.85 x 0.85 = 3.887. Sum = 5.873 + 3.887 = 9.760, rounded up to one decimal: 9.8, Critical. The same code reproduces six published scores.
Then reachability: the store uses json-deserialize and log-viewer, but not xml-entities. So DEMO-2026-0001 (9.8, reachable) is fixed now, DEMO-2026-0003 (6.1, reachable) inside the patch window, and DEMO-2026-0002 (7.5, not reachable) with the next upgrade. A high score nobody can reach is still a debt.

### 4. Patch windows

A patch window, or SLA, is the number of days a fix may take, set by severity: Critical 7, High 30, Medium 90, Low 180. The deadline is the day it was published plus the window. Time is a simulated clock, and today is day 40.
DEMO-2026-0001, Critical, published on day 0, has a deadline of day 7 and was patched on day 5: in time. DEMO-2026-0002, High, published on day 0, had a deadline of day 30 and is still open: breached 10 days ago. DEMO-2026-0003, Medium, published on day 12, has a deadline of day 102: 62 days left.
Two of three are inside the window. The breach is the finding judged not reachable: low-priority work quietly ages unless it has a deadline too.

### 5. Pinning, and checking what you download

Pinning means recording the SHA-256 hash of each artifact when a person reviews it: 3 are pinned. Every later download is hashed again and compared with the pin.
The reviewed acme-json@1.8.3 is VERIFIED. The same name and version with one bit flipped, the first byte 0x61 'a' becoming 0x60 '`', has a completely different hash: MISMATCH, refused. A version nobody has reviewed, acme-json@1.8.4, is NOT_PINNED, refused until someone reviews it.
A name one letter away from a real one, acme-jsom, looks like a copy (a typosquat); acme-json itself does not. Gradle can enforce pins for every dependency with gradle/verification-metadata.xml (described here, not run). The cost: each new hash needs a review, so keep updates small.

## The verdict

Produce an inventory with every build, scan it daily against current advisories, rank by score and reachability, give each severity a deadline and track it, pin and verify artifacts, and prefer a small number of well-maintained dependencies.

## How to recognise this in code you did not write

- Pull requests from a dependency bot.
- `dependencyInsight` and `dependencies` in Gradle, and `mvn dependency:tree`.
- `verification-metadata.xml`, `gradle.lockfile` and `package-lock.json`.
- A security policy that says "Critical within seven days".

## Where you have already met this

- Dependabot, Renovate and similar bots that open pull requests for new versions.
- `npm audit`, `pip-audit`, OWASP Dependency-Check and Trivy, which match an inventory against advisories.
- CycloneDX and SPDX, the standard formats for a software bill of materials.
- Gradle's dependency locking and dependency verification, and `package-lock.json` with its integrity hashes.
- News about supply chain attacks, where a trusted package was replaced or copied under a similar name.
