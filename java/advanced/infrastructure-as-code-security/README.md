# Infrastructure as code security

Scanning and policing the code that builds the infrastructure.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [iac-scanning](iac-scanning) | Write eight rules for Kubernetes manifests with tests of their own, run Checkov, Trivy and KICS on the same files, and handle exceptions and a baseline in the build |
| 2 | [policy-as-code](policy-as-code) | Write a bank's deny-by-default deployment policy in Rego, test the policy and test its tests, and run it as an admission review and in the build with Conftest |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
