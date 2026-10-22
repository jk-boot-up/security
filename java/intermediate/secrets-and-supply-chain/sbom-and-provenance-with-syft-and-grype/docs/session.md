# Session Guide — SBOMs and Scanning with syft and grype

## Learning Objectives

By the end of the session you can:

- write a bill of materials for an image and read it
- scan an image or a bill and read a finding
- write a gate with exceptions that end
- name three things a scanner cannot see

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What a bill of materials answers | 7 min |
| 0:17 | Act 2: Scanning it against known advisories | 7 min |
| 0:24 | Act 3: Fixing it, and checking that it is fixed | 7 min |
| 0:31 | Act 4: A gate that reads the scan | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the version that fixes each advisory. In act four ask why an exception may not cover a critical advisory. In act five ask what a scan of last month's bill would miss today.

## Exercises

1. Add a rule that refuses a library with no licence.
2. Add a statement of exploitability that silences one finding with a reason.
3. Write the bill at the build and keep it with the image's digest.
4. Scan the bill again with an older database and compare.
