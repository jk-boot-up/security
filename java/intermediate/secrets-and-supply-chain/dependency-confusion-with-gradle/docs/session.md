# Session Guide — Dependency Confusion with Gradle

## Learning Objectives

By the end of the session you can:

- explain why a version range lets a public package win
- write a repository content filter
- write and read verification metadata
- name what a filter and a checksum file do not cover

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A build that works | 7 min |
| 0:17 | Act 2: Somebody registers the company's name in the public repository | 7 min |
| 0:24 | Act 3: The repository says which names it may answer for | 7 min |
| 0:31 | Act 4: Checksums that the build checks | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the version that won. In act three look at the count of requests to the public repository. In act five ask which subgroup you forgot in your own build.

## Exercises

1. Use exclusiveContent for one group and compare it with includeGroup.
2. Add a dependency lock and see what it changes.
3. Add the same filters to pluginManagement.
4. Turn a version range into an exact version and rerun act two.
