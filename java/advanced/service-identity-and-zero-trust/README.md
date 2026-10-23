# Service identity and zero trust

Certificates, meshes and workload identity between services.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [mutual-tls-between-services](mutual-tls-between-services) | Make a bank's services prove who they are with client certificates from a private CA, authorize by that identity and not by the network, and rotate short-lived certificates without downtime |
| 2 | [service-mesh-mtls](service-mesh-mtls) | Let a mesh give a bank's pods identities from their ServiceAccounts and encrypt every call, measure it on a real Linkerd cluster, and see what the mesh does not protect |
| 3 | [workload-identity](workload-identity) | Replace a shared secret with SPIFFE IDs and short-lived SVIDs from a real SPIRE server and agent, between two Java services at a bank |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
