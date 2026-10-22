# Detection and response

A breach that nobody notices lasts longest. This module covers OWASP Top 10 (2025) A09: logging what matters and nothing secret, detecting attacks in the logs, and responding when one is found.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [security-logging](security-logging) | Structured security events, secrets kept out of logs, forged lines stopped by structured output, and a hash-chained, append-only log | A09, CWE-778, CWE-117, CWE-532 |
| 2 | [detecting-attacks](detecting-attacks) | A detection rule is a hypothesis about an attacker, and it must be measured against a normal day | A09 |
| 3 | [incident-response](incident-response) | The outcome of an incident is mostly decided before it starts, and by the order of the first hour | A09 |
| 4 | [security-logging-with-loki-and-grafana](security-logging-with-loki-and-grafana) | Ship the bank's security events to a real Loki, ask who moved the money in one line of LogQL, and let a rule watch for password guessing | A09, ASVS V16 |
| 5 | [detecting-attacks-with-opensearch](detecting-attacks-with-opensearch) | The same four detections as saved queries and monitors in a real OpenSearch, scored against one Monday of logs, and the one detection a query cannot say | A09, CSF Detect |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
