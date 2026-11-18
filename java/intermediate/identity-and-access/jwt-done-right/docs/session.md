# Session Guide — JWT Done Right

## Learning Objectives

By the end of the session you can:

- take a JWT apart and say what each part is for
- explain why the algorithm must be chosen by the server
- list the claims an API must check, and why each matters
- describe key rotation with key ids, and why keys are never fetched on a token's say-so
- explain why access tokens must be short-lived

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: How the bank builds a token | 5 min |
| 0:15 | Act 2: Decoding is not verifying | 5 min |
| 0:20 | Act 3: The server chooses the algorithm, never the token | 5 min |
| 0:25 | Act 4: Every claim that matters is checked | 5 min |
| 0:30 | Act 5: Keys by id, from a set the API already holds | 5 min |
| 0:35 | Act 6: The bill: a token cannot be taken back | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Start with the cinema ticket. Decode the real token from act one by hand, with any base64url tool, to show that anyone can read it. Then run the acts. After
act three, ask what would happen if the validator took the algorithm from the header. After act four, ask which claim stops a token for one API being
used at another. In act six, ask how long a stolen access token is useful, and how the bank could shorten that.

## Exercises

1. Add a `jti` (token id) claim and a small deny-list for tokens reported stolen; measure what it costs to check on every request.
2. Require the `scope` claim to contain two scopes for a payment endpoint, and write the refusal test first.
3. Add a third key, rotate twice, and list which tokens still work after each step.
4. Change the skew to zero and to five minutes, and write down what each choice costs.
