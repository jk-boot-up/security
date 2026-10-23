# DevSecOps pipelines

Static and dynamic analysis, secret scanning and a pipeline that fails closed.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [sast](sast) | Write static analysis rules for a bank's Java code, compare a pattern rule with a taint rule, test the rules, and triage the findings with Semgrep |
| 2 | [secret-scanning](secret-scanning) | Find a bank's secrets in the files and in the whole git history, compare a scanner you write with gitleaks, and block the next secret before it leaves the laptop |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
