# Cloud security without a cloud

Identity, network and configuration of a cloud, learned without one: policies evaluated in Java, local emulators and scanners.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [cloud-iam-models](cloud-iam-models) | Evaluate cloud permission policies the way the provider does: deny wins, accounts must agree, roles narrow, and a deputy can be confused |
| 2 | [cloud-misconfigurations](cloud-misconfigurations) | Deploy a bank's first cloud setup to an AWS emulator, find 13 misconfigurations, fix them, and find the same problems in the code before deploy with Checkov and Trivy |
| 3 | [iac-scanning](iac-scanning) | Write eight rules for Kubernetes manifests with tests of their own, run Checkov, Trivy and KICS on the same files, and handle exceptions and a baseline in the build |
| 4 | [network-isolation](network-isolation) | Measure what a taken-over service can reach on a flat network, on coarse networks and on a layout built from the design, and compare the measurement with the intent |
| 5 | [policy-as-code](policy-as-code) | Write a bank's deny-by-default deployment policy in Rego, test the policy and test its tests, and run it as an admission review and in the build with Conftest |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
