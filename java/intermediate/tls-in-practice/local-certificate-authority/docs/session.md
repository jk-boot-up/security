# Session Guide — A Local Certificate Authority

## Learning Objectives

By the end of the session you can:

- name the parts of an X.509 certificate and say what each protects
- explain what a client checks when it validates a chain
- explain why the subject alternative name, not the common name, is used
- describe CRLs, hard fail and why short-lived certificates are attractive

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Issue a certificate and trust the root | 7 min |
| 0:17 | Act 2: What a client refuses | 7 min |
| 0:24 | Act 3: Revocation | 7 min |
| 0:31 | Act 4: Renewal | 7 min |
| 0:38 | Act 5: Short-lived certificates instead of revocation | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask which check refused each certificate. In act three, ask what a client should do when it cannot fetch the list, and what each answer costs. In act five, compare the two exposure windows.

## Exercises

1. Add an intermediate CA between the root and the leaf and validate the three-certificate chain.
2. Add a `pathLen` constraint to the intermediate and test that it cannot issue another intermediate.
3. Add name constraints so that the CA can only issue for *.shop.example.
4. Make renewal use a new key and compare what it protects against.
