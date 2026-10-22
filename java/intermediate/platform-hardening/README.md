# Platform hardening

The code can be right and the machine under it wrong. This module checks the systems that applications run on: a Linux host and a container. Each project writes a checker in Java,
runs it against a prepared system that has the usual mistakes, shows the report, applies the fixes that are safe to automate, and shows what is left for a person to decide.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [linux-hardening](linux-hardening) | Most host compromises begin with a setting that nobody looked at | A02, CWE-732, CWE-250 |
| 2 | [container-hardening](container-hardening) | Assume the process is taken over, and ask what it can reach | A02, CWE-250, CWE-269 |
| 3 | [linux-hardening-with-docker](linux-hardening-with-docker) | Scan a real container the way an auditor scans a host, fix what a machine can fix, and put every decision where it lasts: in the recipe of the image | CIS Benchmarks |

## How the module is judged

- **Real files and real permissions.** The checkers read a prepared directory tree with the JDK's own file APIs.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
