# Session Guide — Password Attacks and Defences with nginx

## Learning Objectives

By the end of the session you can:

- explain what limit_req counts, and what the key is
- explain why a client-written header is a bad key
- explain why a limit per username locks the owner out and misses a spray
- say what a reverse proxy must do with X-Forwarded-For

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: nginx in front of the login, with no limit | 7 min |
| 0:17 | Act 2: limit_req on the client's address: 1r/m, burst 5 | 7 min |
| 0:24 | Act 3: The same limit, but counted on the X-Forwarded-For header | 7 min |
| 0:31 | Act 4: The same limit, counted on the real connection address | 7 min |
| 0:38 | Act 5: The bill: a limit per username | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the door guard analogy. Read `infra/nginx-limit-by-address.conf` aloud first and ask the group what each line does. Then run the demo
one act at a time. After act two, ask what the six means. After act three, ask what the attacker did, before showing the answer. In act five, ask whether
a limit per username could ever be right, and when.

## Exercises

1. Change the burst to 2 and predict the counts before running the demo.
2. Add a key made of the address and the username together. Which attacks does that stop, and which does it miss?
3. Change limit_req_status to 503 and see what changes in the output.
4. Write the nginx lines that overwrite X-Forwarded-For with the real address, and test them.
