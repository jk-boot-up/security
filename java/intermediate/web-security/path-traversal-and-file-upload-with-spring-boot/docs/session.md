# Session Guide — Path Traversal and File Upload with Spring Boot

## Learning Objectives

By the end of the session you can:

- explain why a file name from a request must be treated as a path attack
- compare text checks, path checks and real-path checks
- say what Spring's static handler and multipart limits protect and what they do not
- write an upload and a redirect that cannot be steered by the customer

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An honest name downloads one invoice | 5 min |
| 0:15 | Act 2: A name with .. climbs out of the folder | 5 min |
| 0:20 | Act 3: A text check is not a path check | 5 min |
| 0:25 | Act 4: The fixed version checks in four layers | 5 min |
| 0:30 | Act 5: Spring's static handler follows a link | 5 min |
| 0:35 | Act 6: The naive upload trusts the customer's name | 5 min |
| 0:40 | Act 7: The fixed upload decides for itself | 5 min |
| 0:45 | Act 8: Return links stay on our own site | 5 min |
| 0:50 | Exercises | 10 min |

## Walkthrough

Run act three and ask what startsWith compares. In act five ask why Spring's handler served the link. In act seven ask which of the five uploads checks would still be needed if the limit property were missing.

## Exercises

1. Add a test that fails if `getOriginalFilename()` reaches `Path.resolve`.
2. Make the static handler refuse links by plugging in a resolver that checks the real path.
3. Store the original file name in a field and show it encoded in a page.
4. Add JPEG to the sniffer and a test with a real header.
