# Session Guide — OpenID Connect

## Learning Objectives

By the end of the session you can:

- explain what OpenID Connect adds to OAuth 2.0
- list the checks a relying party makes on an ID token, and what each stops
- explain discovery, and why the metadata must name the configured issuer
- explain why a customer is identified by issuer and subject, not by email
- describe RP-initiated and back-channel logout

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: OAuth gives access; OpenID Connect adds who signed in | 7 min |
| 0:17 | Act 2: Checking the ID token, as OpenID Connect Core requires | 7 min |
| 0:24 | Act 3: Discovery and keys | 7 min |
| 0:31 | Act 4: Who signed in: issuer and subject, never the email | 7 min |
| 0:38 | Act 5: Signing out, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel desk: key card and passport. Run act one and ask what the access token proves. After act two, ask which check stops a token from
another login, and which stops a token for another app. In act three, ask what an attacker gains by serving the store a different metadata document.
In act four, ask which sign-in providers the group uses and whether those sites link accounts by email.

## Exercises

1. Add `max_age` to the sign-in request and refuse an ID token whose `auth_time` is older than it.
2. Add a third provider and a rule that only the bank may be used for payments, by `iss` and `acr`.
3. Make the store cache the key set and reload it only when an unknown key id appears, with a limit on how often.
4. Add `prompt=login` for a payment and check `auth_time` is newer than the request.
