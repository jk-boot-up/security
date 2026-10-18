# Secure design

OWASP Top 10 (2025) A06 Insecure Design: finding threats before code is written, deciding what to do about each, and turning those decisions into checks that keep holding as the design changes.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [threat-modeling](threat-modeling) | A bank as a data-flow model in code, STRIDE threats found per element and boundary, rated and decided, with mitigations kept as tests that fail when the model drifts | A06, SSDF PW.1 |
| 2 | [security-requirements-with-asvs](security-requirements-with-asvs) | Choosing an ASVS level, requirements kept as data, automated checks against a running store, coverage by chapter and a release gate | ASVS 5.0, A06, SSDF PW.1 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
