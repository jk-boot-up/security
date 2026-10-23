# Resilience and recovery

Backups that survive ransomware, and failures injected into controls

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [ransomware-resilience](ransomware-resilience) | Keep a bank's ledger backups where an attacker with the backup credentials cannot destroy them: object lock, separate credentials, a second store, a guard against poisoned backups, and a restore exercise with a recovery time and point |
| 2 | [security-chaos-engineering](security-chaos-engineering) | Inject failures into the security controls of a bank payments service with Toxiproxy and check that they fail closed and that somebody is told |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
