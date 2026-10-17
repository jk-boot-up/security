# Problem Statement

## The scenario

An online store runs its jobs in Kubernetes. Each job has a service account whose token can be stolen from a pod, a log or a backup. The store wants to know how much each stolen token could do, and to keep that small.

## The naive version

Give a job cluster-admin, or a wildcard role, because it fixes the permission error quickly.

## What this project must deliver

- Real RBAC manifests for five identities, from cluster-admin to a role that names one config map.
- An evaluator that answers `kubectl auth can-i` from the manifests, with the blast radius counted.
- The escalation paths of create pods, create role bindings, bind and escalate.
- A demonstration that a namespaced binding keeps a cluster-wide role inside its namespace.
- A linter for cluster-admin bindings, wildcards, escalation verbs and automounted tokens.
- A check against a real kind cluster.
