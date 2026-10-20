# Session Guide — Path Traversal, File Upload and Open Redirects

## Learning Objectives

By the end of the session you can:

- explain how `..`, encoded dots and symbolic links lead out of a folder
- say why comparing paths as text fails and what `Path.startsWith` does differently
- list what an upload handler should decide for itself and what it must never take from the customer
- explain why `//host` and `/\host` are other sites, and why the fixed redirect accepts only simple paths

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Downloading an invoice | 5 min |
| 0:15 | Act 2: A name with .. leaves the folder | 5 min |
| 0:20 | Act 3: A text check on the path is not a path check | 5 min |
| 0:25 | Act 4: Names, paths and links, all checked | 5 min |
| 0:30 | Act 5: Uploads | 5 min |
| 0:35 | Act 6: Where to send the customer next | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run act three and ask what "starts with" compared. In act four, ask which of the layers stopped each attempt. In act five, ask which part of the customer's upload the fixed version actually used.

## Exercises

1. Add a case-insensitive file system rule to the check and test it with `INVOICE-1001.TXT`.
2. Make uploads re-encode the image and compare the bytes.
3. Store the type next to the upload on disk instead of in memory.
4. Add an allow-list of two partner hosts to the redirect and test look-alike names.
