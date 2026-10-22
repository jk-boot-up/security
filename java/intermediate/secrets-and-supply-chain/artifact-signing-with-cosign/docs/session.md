# Session Guide — Artifact Signing with cosign

## Learning Objectives

By the end of the session you can:

- explain why a tag cannot be trusted and a digest can
- verify an image against a pinned key before it runs
- sign a claim and an attestation, and say what a gate must still check
- list the ways around a gate

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A deployment that runs whatever a tag points at | 7 min |
| 0:17 | Act 2: A signed release, checked before it runs | 7 min |
| 0:24 | Act 3: Signed claims, and keys that rotate | 7 min |
| 0:31 | Act 4: Signed statements about how the image was built | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and find the sentence that names the problem. In act three ask what a signature says about when it was made. In act four ask which rule would have stopped the laptop build.

## Exercises

1. Add a rule that refuses provenance with reproducible set to false.
2. Run a local transparency log and record every signature in it.
3. Pull by digest in the deployment file and test that the tag no longer matters.
4. Add an admission check that runs the same gate before a container starts.
