# Session Guide — Finding TLS Misconfigurations

## Learning Objectives

By the end of the session you can:

- list the settings that make a TLS server weak, with a reason for each
- explain forward secrecy and why a suite without it is a finding
- explain why an incomplete chain breaks some clients and not others
- describe what a policy can and cannot fix

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A server that follows the guidance | 8 min |
| 0:18 | Act 2: A legacy server | 8 min |
| 0:26 | Act 3: A certificate that clients will refuse, and a forgotten HTTP setup | 8 min |
| 0:34 | Act 4: Moving the legacy server to a policy | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask which findings a policy file can fix, and which need a new certificate. In act three, ask why a good protocol list does not rescue the pay server. In act four, ask what to check next, a month later.

## Exercises

1. Add a rule for certificates valid for more than 398 days.
2. Add a rule for a missing OCSP stapling flag, and decide its severity.
3. Make the build fail when a report has a finding that is not on an accepted list with a reason.
4. Add a TLS 1.3-only profile for internal services and check it with the modern policy.
