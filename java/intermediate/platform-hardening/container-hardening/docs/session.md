# Session Guide — Container Hardening

## Learning Objectives

By the end of the session you can:

- name the settings that decide what a compromised container process can do
- explain why a non-root user is not enough alone
- list what the Restricted Pod Security level requires
- explain what hardening does not fix

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What 'docker run' with no options gives an attacker | 7 min |
| 0:17 | Act 2: The configuration from the tutorial | 7 min |
| 0:24 | Act 3: One setting at a time | 7 min |
| 0:31 | Act 4: The hardened container | 7 min |
| 0:38 | Act 5: Settings that look harmless | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask which one setting is the worst, and why. In act three, ask why changing the user did not change the count. In act four, ask what the next layers (network policy, secrets management) have to do for the one attack that is left.

## Exercises

1. Add an attack for the service account token mounted at a well-known path, and a setting that blocks it.
2. Add a network egress setting and an attack that uses it to reach the cloud metadata address.
3. Add a custom seccomp profile level between the default and unconfined.
4. Read a Kubernetes YAML file and build a ContainerSpec from it.
