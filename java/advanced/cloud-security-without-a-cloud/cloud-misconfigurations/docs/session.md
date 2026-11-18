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
| 0:10 | Act 1: The first version, deployed to the emulator | 7 min |
| 0:17 | Act 2: The fixes, made with the same SDK | 7 min |
| 0:24 | Act 3: Before deploy: the same setup as files, read by two scanners | 7 min |
| 0:31 | Act 4: The metadata endpoint, reached through the web server | 7 min |
| 0:38 | Act 5: What the scanners and the emulator cannot tell | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and match each of the 13 findings to one line of THE ACCOUNT. In act two count the edits and the replacements. In act three find the two rules that only Checkov matches, and the port neither tool flags. In act four read the three pairs of request and response.

## Exercises

1. Add a tenth rule for a bucket without logging.
2. Make the fixed files pass the customer-managed key checks.
3. Find a Checkov or Trivy check for port 5432, or write one.
4. Add an exception list with an owner and an expiry date.
5. Make the scan a test that fails the build on a critical finding.
