# Problem Statement

## The scenario

An online store is built from a handful of libraries it chose and a larger number it did not. A flaw is announced in one of them, and the team has to say within hours whether the store is affected, how badly, and by when it will be fixed.

## The naive version

Keep no list, upgrade when something breaks, and download whatever the build asks for. Then nobody can say what is in the store, a fix is found by luck, and a swapped download is installed without a murmur.

## What this project must deliver

- A bill of materials in a small JSON file, read by a JSON reader written for the demo.
- Version comparison and ranges that treat versions as numbers.
- A match of the inventory against a local advisory file, with the dependency path and the fix for each finding.
- The real CVSS 3.1 base score equation, checked against published scores.
- Triage by score and reachability, and deadlines on a simulated clock.
- Pinned SHA-256 hashes, a refusal for changed or unreviewed artifacts, and a lookalike-name check.
