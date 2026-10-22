# Session Guide — TLS Misconfigurations with testssl.sh

## Learning Objectives

By the end of the session you can:

- run testssl.sh and read its JSON
- write a rule over a finding
- write an exception with a reason
- say why a scan by address can be wrong

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A server that follows the guidance | 7 min |
| 0:17 | Act 2: A configuration copied from the first search result | 7 min |
| 0:24 | Act 3: A server that still speaks what old clients speak | 7 min |
| 0:31 | Act 4: Certificates that clients will refuse | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and find the severity testssl gives TLS 1.0, then the rule that refuses it. In act five find the scan that fails although the server is fine.

## Exercises

1. Add a rule that requires an OCSP responder, and remove the exception.
2. Add the vulnerability checks (-U) and decide which ones the shop requires.
3. Scan the servers of the previous project with this policy.
4. Fail a build when a certificate has less than 30 days left.
