# Session Guide — OAuth 2.0 Authorization Code Flow with PKCE

## Learning Objectives

By the end of the session you can:

- draw the authorization code flow and say which party sees which secret
- explain what PKCE protects against and why S256 is required
- explain why redirect URIs must match exactly and why state is bound to the browser
- describe refresh token rotation and what reuse detection does
- name the two grants that current practice removes, and why

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The authorization code flow, step by step | 7 min |
| 0:17 | Act 2: PKCE: a code is useless without the verifier | 7 min |
| 0:24 | Act 3: Exact redirect addresses, state, and codes that work once | 7 min |
| 0:31 | Act 4: Scopes and refresh tokens | 7 min |
| 0:38 | Act 5: Current practice, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel key card. Run the demo one act at a time. After act one, ask who saw the password, who saw the code and who saw the token. After act
two, ask where a code can leak in a real phone or browser. In act three, ask what a prefix match would have allowed. In act four, ask why the honest app
loses its tokens too when a thief reuses a refresh token, and whether that is the right trade.

## Exercises

1. Add a second registered redirect address for a test environment and check that each still matches only itself.
2. Make codes last 10 minutes instead of 60 seconds and write the test that shows the risk.
3. Add a `prompt=consent` option that forces the consent question again, and store what each customer agreed to.
4. Change the bank API to accept self-contained JWT access tokens from the jwt-done-right project, and list what is lost when it stops calling the server.
