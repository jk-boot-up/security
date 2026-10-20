# Platform hardening

The code can be right and the machine under it wrong. This module checks the systems that applications run on: a Linux host and a container. Each project writes a checker in Java,
runs it against a prepared system that has the usual mistakes, shows the report, applies the fixes that are safe to automate, and shows what is left for a person to decide.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [linux-hardening](linux-hardening) | Most host compromises begin with a setting that nobody looked at | A02, CWE-732, CWE-250 |

## How the module is judged

- **Real files and real permissions.** The checkers read a prepared directory tree with the JDK's own file APIs.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
