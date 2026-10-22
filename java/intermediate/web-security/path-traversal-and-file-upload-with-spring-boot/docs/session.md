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
| 0:10 | Act 1: Downloading an invoice | 5 min |
| 0:15 | Act 2: A name with dot-dot leaves the folder | 5 min |
| 0:20 | Act 3: A text check on the path is not a path check | 5 min |
| 0:25 | Act 4: Names, paths and links, all checked | 5 min |
| 0:30 | Act 5: Uploads | 5 min |
| 0:35 | Act 6: Where to send the customer next | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run act three and ask what startsWith compares. In act four ask why Spring's handler served the link. In act five ask which of the five uploads checks would still be needed if the limit property were missing.

## Exercises

1. Add a test that fails if `getOriginalFilename()` reaches `Path.resolve`.
2. Make the static handler refuse links by plugging in a resolver that checks the real path.
3. Store the original file name in a field and show it encoded in a page.
4. Add JPEG to the sniffer and a test with a real header.
