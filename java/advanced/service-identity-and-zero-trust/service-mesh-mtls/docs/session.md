# Session Guide — Service Mesh and Automatic mTLS

## Learning Objectives

By the end of the session you can:

- show that pod-to-pod traffic is plaintext before a mesh
- read a workload identity from its certificate
- write a policy that names the one identity allowed
- list what a mesh does not protect

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Before and after the proxies | 7 min |
| 0:17 | Act 2: Identities from ServiceAccounts | 7 min |
| 0:24 | Act 3: Mutual TLS is not a decision | 7 min |
| 0:31 | Act 4: A policy names who may call | 7 min |
| 0:38 | Act 5: What the mesh does not protect | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the third and fourth acts: the same sixteen calls before and after the policy. Open Policies.java and read the manifests for the ledger.

## Exercises

1. Add a second caller to the ledger policy and measure again.
2. Make the batch job part of the mesh and see what its identity is.
3. Set the default inbound policy of the namespace to deny and compare.
4. Lower the certificate lifetime and watch a renewal fail when the identity service is stopped.
