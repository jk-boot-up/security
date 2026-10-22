# Session Guide — SAML Basics with Keycloak

## Learning Objectives

By the end of the session you can:

- name the parts of a SAML response and which are signed
- explain why a valid signature is not enough
- list the checks on the statement itself
- plan a key rotation with a service provider that reads metadata once

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Signing in through the bank | 7 min |
| 0:17 | Act 2: A valid signature is not the same as the bank's signature | 7 min |
| 0:24 | Act 3: A valid signature on one assertion, and another one read | 7 min |
| 0:31 | Act 4: The statement itself is checked | 7 min |
| 0:38 | Act 5: The bill: keys, metadata and XML | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask what the shop compares the signature with. In act four ask what a replay would need to succeed. In act five ask who must be told before the bank rotates its key.

## Exercises

1. Make the shop reload the identity provider's metadata every hour and test the rotation again.
2. Make the bank sign the authentication request too, and register the shop's certificate.
3. Add single logout and test that one sign-out reaches both sides.
4. Map the attributes of the assertion to the shop's roles and test a role that the bank did not send.
