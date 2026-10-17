# Session Guide — Password Hashing with Spring Security Crypto

## Learning Objectives

By the end of the session you can:

- name the three things a Spring PasswordEncoder gives you that the plain project wrote by hand
- explain what the {bcrypt} in front of a stored text is for
- describe how a shop moves to a stronger algorithm without a password reset
- explain why an allow-list of algorithms is safer than the default list

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Two the library marks as deprecated | 5 min |
| 0:15 | Act 2: One call: the delegating encoder | 5 min |
| 0:20 | Act 3: Four algorithms, one interface | 5 min |
| 0:25 | Act 4: The same attack, at two costs | 5 min |
| 0:30 | Act 5: Moving to a stronger algorithm without asking anyone | 5 min |
| 0:35 | Act 6: The bill: an open list, and memory | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the plain project and this one side by side and compare act four with act four: the same twenty four guesses, the
same four accounts. Then read act five slowly: ask the group what happens to a customer who never comes back, and
what the shop could do about it. In act six, ask who can write to the accounts table in a real shop, and what else
they could do if they can.

## Exercises

1. Change the default to scrypt and run act five again. What does the new prefix look like?
2. Add a `{pbkdf2}` entry to the allow-list and make the shop upgrade Dave's account from it.
3. Increase bcrypt's cost to 12 in act four. How much more work is that, compared with cost 10?
4. Write a test that fails if the default delegating encoder is used anywhere in the demo's code.
