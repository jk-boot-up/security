# Service identity and zero trust

Certificates, meshes and workload identity between services.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [mutual-tls-between-services](mutual-tls-between-services) | Make a bank's services prove who they are with client certificates from a private CA, authorize by that identity and not by the network, and rotate short-lived certificates without downtime |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
