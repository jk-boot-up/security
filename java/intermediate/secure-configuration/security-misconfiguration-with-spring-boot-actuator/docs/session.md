# Session Guide — Security Misconfiguration with Spring Boot Actuator

## Learning Objectives

By the end of the session you can:

- name what each Actuator endpoint gives a stranger
- explain what Spring Boot hides by default and what it cannot hide
- separate operations from customers by port, list, login rules and access level
- run a checker against a live store and read its findings

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Expose everything | 7 min |
| 0:17 | Act 2: What Spring Boot already does | 7 min |
| 0:24 | Act 3: Hardened: another port, a short list, rules | 7 min |
| 0:31 | Act 4: Who may read, and who may change | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask which endpoint hurts most. In act two ask why masking does not help once a heap dump is allowed. In act three ask which of the four settings you would lose first in a rushed deployment.

## Exercises

1. Add a ninth check: the info endpoint shows no build or git details to strangers.
2. Put the operations port behind a second interface address and test that the customers' address refuses it.
3. Add a custom `@Endpoint` that lists open refunds, and decide its role and its access level.
4. Write a test that fails whenever a new endpoint appears in the operator's list.
