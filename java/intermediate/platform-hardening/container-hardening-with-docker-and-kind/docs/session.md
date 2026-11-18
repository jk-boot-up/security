# Session Guide — Container Hardening with Docker and kind

## Learning Objectives

By the end of the session you can:

- assume a process is taken over and measure what it can reach
- name the docker run options that take attacks away
- read a Pod Security Admission refusal
- say what is left in the hardened container and why

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What docker run with no options gives an attacker | 5 min |
| 0:15 | Act 2: The configuration from the tutorial | 5 min |
| 0:20 | Act 3: One setting at a time | 5 min |
| 0:25 | Act 4: The hardened container | 5 min |
| 0:30 | Act 5: Settings that look harmless | 5 min |
| 0:35 | Act 6: The same on Kubernetes, with Pod Security Standards on kind | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run act three and find the line where the number does not change although the set does. In act five find the layer that holds when root comes back. In act six find the two attacks left in the hardened pod.

## Exercises

1. Add a fourteenth attempt and see which option stops it.
2. Mount the secret as a file instead of an environment variable and rerun the probe.
3. Add a seccomp profile to the Kubernetes pod and compare.
4. Move the hardened pod's temporary directory to a noexec mount.
