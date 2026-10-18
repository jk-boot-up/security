# Secure coding

Habits in the code itself: validating input at the trust boundary, keeping state changes atomic and business rules on the server, keeping secrets out of errors and logs, and finding bugs with fuzzing.

## The projects

| # | Project | What it shows | Maps to |
| --- | --- | --- | --- |
| 1 | [input-validation](input-validation) | Server-side validation, typed allow-lists, canonical forms for Unicode names, size limits and regular-expression denial of service | A05, A06, CWE-20, CWE-1333, CWE-176 |
| 2 | [race-conditions-and-business-logic](race-conditions-and-business-logic) | Two withdrawals that overdraw, a coupon used twice and a retried payment; locks, compare-and-set, idempotency keys, server-side rules and lock ordering | A06, CWE-362, CWE-367, CWE-840 |

A project with `-with-` in its name is the same lesson with a framework or a local tool, and is its own self-contained project.

## How the module is judged

- **Break it, then fix it,** or build the control and prove each refusal with a test.
- **Every claim is a number** that a test checks against a golden output file.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

See the [intermediate plan](../../../docs/intermediate-plan.md) for the projects still to come.
