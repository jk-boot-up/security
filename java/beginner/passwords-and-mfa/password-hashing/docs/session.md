# Session Guide — Password Hashing

## Learning Objectives

By the end of the session you can:

- explain why a shop stores a hash and never the password
- explain what a salt does and what it does not do
- explain why a slow hash helps, and why it cannot save a weak password
- describe how an old hash is upgraded without asking the customer

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Passwords stored as they are | 7 min |
| 0:17 | Act 2: A fast hash, with no salt | 7 min |
| 0:24 | Act 3: A fast hash, with a salt for each account | 7 min |
| 0:31 | Act 4: A slow hash, with a salt for each account | 7 min |
| 0:38 | Act 5: The bill: every login pays, and old hashes must be upgraded | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the safe analogy. Run the demo one act at a time. After act one, ask what the thief
learns. After act two, ask why Ana and Ben's rows look the same, and why that matters. After act
three, ask why salts do not make the attack much harder. After act four, ask what would make Ana's account
safe, and let the group conclude that the password itself must be stronger. In act five, ask
when old hashes can be upgraded, and why not all at once.

## Exercises

1. Add a stronger customer to the demo with a password not on the list, and show that act four cannot crack it.
2. Change CURRENT_ROUNDS to 200000 and note how the work in act four changes. Which tests change?
3. Add a method that reports which accounts still use fewer rounds than the current setting.
4. Make the attacker's list longer by adding five more words, and count the guesses again.
