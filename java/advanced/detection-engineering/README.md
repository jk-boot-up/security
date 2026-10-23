# Detection engineering

Detections as code, correlation and hunting

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [detection-as-code](detection-as-code) | Write a bank's detections as Sigma rules tagged with MITRE ATT&CK, test them against a week of labelled logs, hold each to a false-positive budget, and run the pySigma-converted queries in OpenSearch |

The module plan is in [`../../../docs/advanced-plan.md`](../../../docs/advanced-plan.md). Run a project as in the other tracks: `./gradlew test`, then `./gradlew run`.
