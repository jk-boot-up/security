# Secure configuration

Most breaches need no clever exploit: a default password, a debug page or a setting copied from development is enough. This module covers
OWASP Top 10 (2025) A02 Security Misconfiguration and A10 Mishandling of Exceptional Conditions: settings that are safe by default, checked
continuously, and code that fails closed when something goes wrong.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [security-misconfiguration](security-misconfiguration) | Default credentials, a debug page, directory listing and stack traces found by a checker; a hardened profile, a startup gate and a drift check | A02, CWE-16, CWE-1188, CWE-209, CWE-548 |
| 2 | [fail-closed-error-handling](fail-closed-error-handling) | A payment check that fails open when a service is down; default deny, timeouts, all-or-nothing transfers, safe error pages and a circuit breaker that fails closed | A10, CWE-636, CWE-755 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## Still to come

security-misconfiguration-with-spring-boot-actuator and fail-closed-error-handling-with-spring-boot, as listed in the
[intermediate plan](../../../docs/intermediate-plan.md).

## How the module is judged

- **Break it, then fix it.** The shipped defaults fail first; the hardened profile passes the same checks.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.
