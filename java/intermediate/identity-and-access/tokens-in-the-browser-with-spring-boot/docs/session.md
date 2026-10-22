# Session Guide — Tokens in the Browser with Spring Boot: a BFF and DPoP

## Learning Objectives

By the end of the session you can:

- explain what a script can read in each design
- build the cookie that a BFF gives the browser, and say why each attribute is there
- explain what DPoP stops and what it does not
- decide when the extra server of a BFF is worth it

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

Run act one and list what the script took. In act two ask what the script can still do while the page is open. In act four ask which of the six refused requests a plain bearer token would have allowed.

## Exercises

1. Move the BFF's authorized clients from memory to a shared store, and run two BFF servers behind one address.
2. Add a Content-Security-Policy to the BFF's pages and test that an inline script no longer runs.
3. Make the accounts API require DPoP for the payments scope only.
4. Add a DPoP nonce from the authorization server and handle the retry.
