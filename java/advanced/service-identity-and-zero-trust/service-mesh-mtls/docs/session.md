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
| 0:10 | Act 1: Before and after the proxies | 5 min |
| 0:15 | Act 2: An identity from each ServiceAccount | 5 min |
| 0:20 | Act 3: Mutual TLS alone does not decide | 5 min |
| 0:25 | Act 4: A policy names who may call | 5 min |
| 0:30 | Act 5: What the mesh does not protect | 5 min |
| 0:35 | Act 6: Certificate lifetime and renewal | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the demo and compare the third and fourth acts: the same sixteen calls before and after the policy. Open Policies.java and read the manifests for the ledger.

## Exercises

1. Add a second caller to the ledger policy and measure again.
2. Make the batch job part of the mesh and see what its identity is.
3. Set the default inbound policy of the namespace to deny and compare.
4. Lower the certificate lifetime and watch a renewal fail when the identity service is stopped.
