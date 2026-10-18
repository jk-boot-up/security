# Session Guide — Tokens in the Browser

## Learning Objectives

By the end of the session you can:

- explain why tokens in localStorage are readable by every script on the page
- describe how a backend for the frontend keeps tokens on the server
- list what stops a request started by another site
- explain what a DPoP proof contains and what it stops
- say what neither the BFF nor DPoP stops

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Tokens kept in the browser | 7 min |
| 0:17 | Act 2: A backend for the frontend keeps the tokens on the server | 7 min |
| 0:24 | Act 3: Requests started by another site | 7 min |
| 0:31 | Act 4: Tokens bound to a key, with DPoP | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel key. Run the demo one act at a time. After act one, ask why the refresh token is worse to lose than the access token. After act two, ask what
the script can still do, and when it stops. In act four, ask which part of the proof stops each refusal. End with act five and the open page.

## Exercises

1. Shorten the refresh token's life to one day and rerun act one.
2. Add a check that the BFF refuses any request whose Origin header is not the app's own.
3. Bind the BFF's own tokens to a server key with DPoP, so a token copied from the BFF's store is useless too.
4. Add a server-supplied nonce (RFC 9449 section 8) to the proofs and refuse proofs without the current one.
