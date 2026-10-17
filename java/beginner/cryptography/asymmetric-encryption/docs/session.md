# Session Guide — Asymmetric Encryption (RSA and Key Agreement)

## Learning Objectives

By the end of the session you can:

- explain the key-sharing problem that asymmetric encryption solves
- explain a public key and a private key, and what each is for
- say why hybrid encryption is used, and what RSA carries in it
- explain what key agreement does, and why a man in the middle defeats the methods in this project

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The problem: sharing a key | 7 min |
| 0:17 | Act 2: A key pair: lock with the public key, open with the private key | 7 min |
| 0:24 | Act 3: Hybrid encryption: RSA for the key, AES for the data | 7 min |
| 0:31 | Act 4: Key agreement: no key is sent at all | 7 min |
| 0:38 | Act 5: The bill: whose key is it? | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the mailbox analogy. Run the demo one act at a time. After act one, ask the group how the bank could give Ana the key safely. After act two, ask what Eve has, and what she lacks. In act four, ask the group what Eve would
need to make the same key, and let them see that she needs a private key she does not have. In act five, ask how Ana could know the public key is really hers, and keep that question for the signatures project.

## Exercises

1. Raise the RSA key size to 3072 and find which numbers in the demo change.
2. Change `HybridBox` to bind the account in the associated data and test a message moved to another account.
3. Make the key agreement derive two keys, one for each direction, with different `info` text.
4. Write a test that fails if RSA is ever used without OAEP padding.
