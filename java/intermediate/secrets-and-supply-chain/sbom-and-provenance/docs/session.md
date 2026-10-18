# Session Guide — SBOMs and Build Provenance: What Is Inside, Who Built It, and From What

## Learning Objectives

By the end of the session you can:

- explain what an SBOM records and what it cannot
- read an in-toto statement with a SLSA Provenance predicate
- explain what the DSSE encoding protects against
- list the checks a deployment gate makes on provenance
- place a pipeline at a SLSA build level and say what holds it back

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What a bill of materials answers, and what it does not | 7 min |
| 0:17 | Act 2: Provenance: who built it, from what | 7 min |
| 0:24 | Act 3: Checking before deploying | 7 min |
| 0:31 | Act 4: SLSA build levels, and builds that can be repeated | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the food label and the certificate of origin. Run act one and ask what the SBOM would tell an auditor and what it would not. In act two, read the statement
aloud field by field. In act three, ask which check stops each refused build. In act four, place the group's own pipeline at a level. In act five, ask who in the
organisation can reach the builder's key.

## Exercises

1. Add a check that the commit in the provenance is one the store has reviewed, from a list of reviewed commits.
2. Write an SPDX version of the SBOM and compare what each format records.
3. Add a second signature to the envelope and require both, as a two-builder rule.
4. Make the careless build reproducible step by step and see which change fixes the digest.
