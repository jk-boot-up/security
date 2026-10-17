# Session Guide — Cross-Site Request Forgery (CSRF) and Its Defences

## Learning Objectives

By the end of the session you can:

- explain why a cookie alone cannot prove that a request was intended
- describe the synchronizer token pattern and why another site cannot read the token
- say what SameSite Lax and Strict allow and refuse
- explain what Sec-Fetch-Site and Origin add, and why they do not replace the token
- explain why GET must never change state

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A request Ana did not intend | 7 min |
| 0:17 | Act 2: A token in every form | 7 min |
| 0:24 | Act 3: SameSite cookies | 7 min |
| 0:31 | Act 4: Where the request came from, and GET | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the signed note in a pile. Run act one, and ask who sent the transfer. Run act two, and ask how the bank's page could leak its token and what would follow.
In act three, ask why Lax is the default and not Strict. In act five, ask which of the defences would stop the reviews page, and why only one does.

## Exercises

1. Add a second form, for changing the e-mail address, and make sure it is protected without copying code.
2. Make the bank accept JSON from a script, and check that it still needs the token or a custom header.
3. Change the bank to refuse requests with no Sec-Fetch-Site and no Origin, and find which real clients that would break.
4. Rotate the token after each transfer and see what happens with two tabs open.
