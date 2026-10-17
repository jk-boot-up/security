# Session Guide — Least Privilege with Kubernetes RBAC

## Learning Objectives

By the end of the session you can:

- explain deny by default and how roles and bindings combine
- count the blast radius of a service account
- name three verbs that allow escalation
- say why the binding, not the role, sets the scope

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The nightly job that was given cluster-admin | 7 min |
| 0:17 | Act 2: The wildcard trap: read everything | 7 min |
| 0:24 | Act 3: Ways to become more powerful | 7 min |
| 0:31 | Act 4: A cluster-wide role through a namespaced binding | 7 min |
| 0:38 | Act 5: A linter for the habits that cause incidents | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the master key analogy. Run the demo one act at a time and open `infra/rbac/shop.yaml` beside it. After act two, ask what else "everything" would include in a cluster that has custom resources. After act three, ask how to let a pipeline deploy without giving it the power to read secrets.

## Exercises

1. Change the reporter's role to name pods and services only, and check its blast radius.
2. Add a RoleBinding that gives the support agent shop-viewer in warehouse and see act four change.
3. Run `kubectl auth can-i --list` as the exporter in a kind cluster and compare it with the manifest.
4. Add a lint rule for ClusterRoleBindings that name the default service account.
