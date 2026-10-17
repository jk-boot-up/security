# Session Guide — Authentication vs Authorization with Keycloak

## Learning Objectives

By the end of the session you can:

- explain what a login server and a resource server each do
- say what a JWT contains and which parts the shop must check
- explain why no token is 401 and a wrong role is 403
- explain why roles in a token can be stale, and the control for it
- say what it costs to end a token early

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: No login in the shop, only tokens | 7 min |
| 0:17 | Act 2: Who is it, and what may they do | 7 min |
| 0:24 | Act 3: A real signature is not enough | 7 min |
| 0:31 | Act 4: The roles in a token are a copy | 7 min |
| 0:38 | Act 5: Ending a token early, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the theatre. Run the demo one act at a time. After act two, ask what the shop would need to store if it did its own logins. After act three, ask what an attacker would try with a token stolen from another API.
In act four, ask how short a lifetime should be and what that costs the login server. In act five, ask whether the shop would ever want to ask the login server about every request.

## Exercises

1. Change the token lifetime in the demo to 60 seconds and rerun act four.
2. Add a new role, `auditor`, that may read every order but not the sales report.
3. Remove the audience check from the decoder and see which tests fail.
4. Start Keycloak with the realm file, open its admin console, and compare a token from the login page with the claims the shop reads.
