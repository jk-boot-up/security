# Governance risk and compliance



## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [risk-management](risk-management) | Quantify a bank's risk register with ranges and a Monte Carlo simulation, choose controls within a budget, and check the residual risk against an appetite |
| 2 | [controls-and-evidence](controls-and-evidence) | Map a bank's controls to NIST CSF 2.0 and ISO 27001, collect evidence from test results, and see which controls are passing, failing, stale or missing |
| 3 | [security-architecture-review](security-architecture-review) | Review a bank's design written in the Structurizr DSL with rules over trust zones, flows, data stores and decision records, and see what no rule can find |
| 4 | [privacy-engineering](privacy-engineering) | Minimize, pseudonymize, retain and erase a bank's customer data in PostgreSQL, measure who can still be picked out, and find the copies that an erasure request misses |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
