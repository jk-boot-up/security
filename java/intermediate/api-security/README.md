# API security

APIs are where most data now leaves a system. This module follows the OWASP API Security Top 10 (2023): what an API must check on every request, how sessions and cross-origin access are kept safe, and how a service trusts messages from others.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [webhook-security](webhook-security) | A payment webhook anyone could forge or replay; HMAC signatures over the raw body, a time window, a record of processed events, idempotent handling and secret rotation | API8, API10, CWE-345, CWE-294 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
