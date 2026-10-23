# Session Guide — Kubernetes Network Policies

## Learning Objectives

By the end of the session you can:

- write a default deny and an allow for one conversation
- measure every pair of pods against a design
- explain why a typo in a label is silent
- explain why egress rules need DNS

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A flat network | 7 min |
| 0:17 | Act 2: Default deny, then the design | 7 min |
| 0:24 | Act 3: Mistakes the API server accepts | 7 min |
| 0:31 | Act 4: Egress and DNS | 7 min |
| 0:38 | Act 5: The label is the credential | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the first and the second act. In the third act find the pair that each mistake opens or closes.

## Exercises

1. Add a policy that lets the admin pod reach the api, and find what else it opens.
2. Make the DNS allow narrower and check the lookup still works.
3. Write a test that fails the build when the measurement differs from the design.
4. Block the relabelling with an admission policy from kubernetes-admission-control.
