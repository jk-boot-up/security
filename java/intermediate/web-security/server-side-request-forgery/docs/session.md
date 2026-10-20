# Session Guide — Server-Side Request Forgery (SSRF) and Its Defences

## Learning Objectives

By the end of the session you can:

- explain why a fetch made by the server reaches things the customer cannot
- list ways of writing the same address that defeat a text blocklist
- describe DNS rebinding and why the checked address must be the connected address
- say why every redirect and every resolved address must be checked

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Importing an image | 7 min |
| 0:17 | Act 2: The server fetches whatever it is told | 7 min |
| 0:24 | Act 3: A blocklist of names is easy to walk around | 7 min |
| 0:31 | Act 4: Check the address the connection really uses | 7 min |
| 0:38 | Act 5: Why checking and then connecting is not enough | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask whose request reached the metadata address. In act three, ask what the blocklist compared and what the connection used. In act five, ask which of the
two lookups the check saw.

## Exercises

1. Add an allow-list of image hosts and see which attacks it makes unnecessary.
2. Add IPv6 unique-local and documentation ranges to the policy, with tests.
3. Make the importer refuse an answer whose address changed between two lookups, and explain why that is weaker than connecting to the checked address.
4. Add a size limit that is checked while reading, not after.
