# Session Guide — JWT Done Right with Nimbus JOSE + JWT

## Learning Objectives

By the end of the session you can:

- configure a Nimbus processor with a fixed algorithm, type, key source and claims verifier
- explain why jku and jwk headers are never followed
- plan a key rotation around the cache time
- name the checks the library does not do

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Decoding is not verifying | 7 min |
| 0:17 | Act 2: The server chooses the algorithm | 7 min |
| 0:24 | Act 3: Every claim that matters | 7 min |
| 0:31 | Act 4: Keys by id, from one URL | 7 min |
| 0:38 | Act 5: The bill: a token cannot be taken back | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the two lines of configuration that refuse the token. In act four ask how long a stolen key keeps working after the bank retires it. In act five ask which of your own APIs needs a revocation list.

## Exercises

1. Add a jti check against a small deny list and decide how long to keep an entry.
2. Make the key cache time 10 minutes and write down the compromise plan it implies.
3. Accept two token types and two algorithms for a migration, and then remove one.
4. Replace ES256 with EdDSA and see which extra dependency Nimbus needs.
