# Session Guide — OAuth 2.0 Authorization Code with PKCE and Keycloak

## Learning Objectives

By the end of the session you can:

- name the parts of the authorization code flow and what each one protects
- explain what PKCE stops and why a failed try burns the code
- list the client settings that make the flow safe
- explain what an online check sees that a local check cannot

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The flow, step by step | 7 min |
| 0:17 | Act 2: PKCE: a code is useless without the verifier | 7 min |
| 0:24 | Act 3: Exact redirects, state, and codes that work once | 7 min |
| 0:31 | Act 4: Scopes and refresh tokens | 7 min |
| 0:38 | Act 5: Current practice, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and follow the code from the redirect to the token. In act two ask what the thief learned from the failed try. In act three ask which token the API will still accept after the session has ended.

## Exercises

1. Register a second redirect address and test the lookalikes of it.
2. Make the resource server introspect tokens online and compare the cost and the behaviour.
3. Turn off refresh token rotation in the realm and see which line of the demo changes.
4. Add a scope that needs a second sign-in step, and test that the app asks again.
