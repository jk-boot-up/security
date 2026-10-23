# Kubernetes and container security

Admission, network policy, runtime detection and secrets on a local cluster.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [kubernetes-admission-control](kubernetes-admission-control) | Stop a bank's unsafe workloads at the door of a real Kubernetes cluster with Kyverno policies that validate, mutate, verify signed images and take exceptions |
| 2 | [kubernetes-network-policies](kubernetes-network-policies) | Default-deny a bank's pods on a real Kubernetes cluster, allow only what the design lists, and measure every pair of pods against the design |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run` (these make a cluster with kind and need Docker).
