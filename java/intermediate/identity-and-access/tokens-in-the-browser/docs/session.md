# Session Guide — Tokens in the Browser: BFF and DPoP

## Learning Objectives

By the end of the session you can:

- explain why a token in web storage is lost to one injected script
- describe how a BFF keeps tokens off the browser and what it needs against CSRF
- say what an HttpOnly cookie does and does not protect
- explain what a DPoP proof contains and which attacks each field stops

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A token in local storage | 8 min |
| 0:18 | Act 2: One injected script takes the token away | 8 min |
| 0:26 | Act 3: A backend for the frontend keeps the token | 8 min |
| 0:34 | Act 4: A token that needs the key as well (DPoP) | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask what the attacker needed besides the token. In act three, ask what an injected script can still do and why that does not make the BFF pointless. In act four, ask which field of the proof stops each refused call.

## Exercises

1. Add a nonce from the API that every proof must include, and see what it adds to the iat check.
2. Rotate the BFF session id after sign-in and after a privilege change.
3. Make the BFF refuse sessions older than eight hours or idle for 30 minutes.
4. Add an HttpOnly refresh token to the BFF, and decide what happens when the access token expires.
