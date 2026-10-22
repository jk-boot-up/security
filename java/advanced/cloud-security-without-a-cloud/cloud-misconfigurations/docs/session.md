# Session Guide — Cloud Misconfigurations

## Learning Objectives

By the end of the session you can:

- name the common cloud misconfigurations
- scan an account and fix the findings
- scan infrastructure files before deploy
- explain IMDSv1, IMDSv2 and what neither stops

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The first version | 7 min |
| 0:17 | Act 2: The fixes | 7 min |
| 0:24 | Act 3: Before deploy: Checkov and Trivy | 7 min |
| 0:31 | Act 4: The metadata endpoint | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and group the 13 findings into their nine kinds. In act three find the two kinds that Trivy misses. In act four read the three IMDS lines.

## Exercises

1. Add a tenth rule for a bucket without logging.
2. Make the fixed files pass the customer-managed key checks.
3. Add an exception list with an owner and an expiry date.
4. Make the scan a test that fails the build on a critical finding.
