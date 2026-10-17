# Patching and Dependency Hygiene, Explained

## The idea in one sentence

Most of a program is other people's code, so a team must list what it ships, match it against known problems, fix the reachable and severe ones first within deadlines, and check that every download matches a hash it reviewed.

## The 5 acts

### 1. An inventory of what the store ships

The store's software lists what it is built from in a small JSON file: 7 components, the store itself and 6 libraries. Three libraries are direct choices, acme-http, acme-image and acme-log. Three arrive on their own, as dependencies of dependencies:
acme-json and acme-crypto through acme-http, and acme-xml through acme-image. Nobody on the team chose them, and a flaw in any of them is still a flaw in the store.

### 2. Matching the inventory against known problems

The advisory file lists five known problems, all fictional. Versions are compared as numbers: as text, 1.10.0 sorts before 1.9.0, as versions it is newer. A range such as at least 1.5.0 and below 1.8.3 contains 1.8.2 but not 1.8.3, the fixed version.
Three of the five apply to this store. Two are in transitive libraries, reached through acme-http and acme-image, so the fix is a constraint that forces the fixed version. One is a direct choice: change acme-log from 4.2.0 to 4.2.1.
The other two do not apply, because the installed acme-crypto and acme-http are already the fixed versions.

### 3. How bad, and what to fix first

The score comes from the CVSS 3.1 equation, which here reproduces six published scores: 9.8, 10.0, 7.8, 5.3, 6.1 and 7.5. The three problems score 9.8 Critical, 7.5 High and 6.1 Medium.
Then the store asks whether it uses the flawed feature. It uses json-deserialize and log-viewer, and does not use xml-entities. So the order is: the critical reachable one first, fix now; then the medium reachable one, inside the patch window;
and the high one that cannot be reached is scheduled with the next upgrade. A high score that cannot be reached is still a debt: schedule it, do not ignore it.

### 4. Patch windows

Each severity has a deadline: Critical 7 days, High 30, Medium 90, Low 180. Time is counted in days on a simulated clock, and today is day 40. The critical problem was published on day 0 and patched in 5 days, inside its 7.
The high problem was also published on day 0, has been open for 40 days, and breached its 30-day deadline 10 days ago. The medium problem was published on day 12 and has been open 28 days, with 62 left. Two of three are inside the window.
The breach is the finding judged not reachable: low-priority work quietly ages unless it has a deadline too.

### 5. Pinning, and checking what you download

A fix means downloading a new artifact, and a download can be swapped. When a person reviews an artifact, the SHA-256 of it is recorded: three are pinned. The same artifact downloaded again is verified. With one bit changed, same name and version,
it is refused as a mismatch. A version nobody has reviewed, acme-json 1.8.4, is refused until someone does. A name one letter away from a real one, acme-jsom, looks like a copy, which is how a lookalike package is published, while acme-json itself does not.
Gradle can enforce hashes for every dependency from a file called verification-metadata.xml; that is described, not run. The cost is a review of every new hash, so automate the pull requests and keep each review small.

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
