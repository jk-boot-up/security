# Session Guide — OpenID Connect with Spring Security and Keycloak

## Learning Objectives

By the end of the session you can:

- explain the difference between an ID token and an access token
- name what Spring Security checks in an ID token and what the store must still do
- key customers by issuer and subject
- configure back-channel logout and say why the store needs its own address

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: OAuth gives access; OpenID Connect adds who signed in | 7 min |
| 0:17 | Act 2: Checking the ID token | 7 min |
| 0:24 | Act 3: Discovery and keys | 7 min |
| 0:31 | Act 4: Who signed in: issuer and subject, never the email | 7 min |
| 0:38 | Act 5: Signing out, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and find the nonce in both the request and the token. In act four ask which providers could say that an e-mail address is theirs. In act five ask what ends when the provider ends a session, and what does not.

## Exercises

1. Add a third provider and decide what the store does when two providers claim the same verified e-mail address.
2. Link accounts on purpose: a signed-in customer adds a second provider to their account.
3. Make the store refuse sign-ins whose acr is below a level it needs for payments.
4. Make back-channel logout work behind a proxy where the store's public address differs from its own.
