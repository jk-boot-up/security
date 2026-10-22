# Session Guide — Server-Side Request Forgery with Spring Boot

## Learning Objectives

By the end of the session you can:

- explain why a server that fetches a customer's address is reachable from outside
- show why a blocklist of names fails against the resolver
- judge every resolved address and connect to the judged address
- name the limits that go with the address check

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Importing an image from a web address | 7 min |
| 0:17 | Act 2: The server fetches whatever it is told | 7 min |
| 0:24 | Act 3: A blocklist of names is easy to walk around | 7 min |
| 0:31 | Act 4: Check the address the connection really uses | 7 min |
| 0:38 | Act 5: Limits, and why checking and then connecting is not enough | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and ask what the blocklist compares and what the network connects to. In act four ask which address the connection used. In act five ask what happens if the lookup is repeated.

## Exercises

1. Add an allow-list of image hosts and decide which check comes first.
2. Make the importer handle HTTPS and keep the host name for the certificate check.
3. Add a test that every address in the policy table of private ranges is refused.
4. Add a per-user rate limit on imports.
