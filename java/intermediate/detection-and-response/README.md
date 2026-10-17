# Detection and response

A breach that nobody notices lasts longest. This module covers OWASP Top 10 (2025) A09: logging what matters and nothing secret, detecting attacks in the logs, and responding when one is found.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [security-logging](security-logging) | Structured security events, secrets kept out of logs, forged lines stopped by structured output, and a hash-chained, append-only log | A09, CWE-778, CWE-117, CWE-532 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
