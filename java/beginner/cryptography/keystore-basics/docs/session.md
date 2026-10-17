# Session Guide — Keystores and Key Management Basics

## Learning Objectives

By the end of the session you can:

- name four places a key can live, and the weakness of each
- explain what a keystore protects, and what it moves to the password
- explain why messages should carry the name of their key
- list what must happen, in order, before an old key is removed

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A key written in the source code | 7 min |
| 0:17 | Act 2: A key in a file outside the program | 7 min |
| 0:24 | Act 3: A PKCS12 keystore: an encrypted file of keys | 7 min |
| 0:31 | Act 4: Making keys from other secrets | 7 min |
| 0:38 | Act 5: Replacing a key, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the vault and the doormat. Run the demo one act at a time. After act one, ask the group who has the key and how to take it back. After act two, ask what the permissions protect against and what they cannot. In act three, ask where the keystore's password should live, and watch the group
discover that it is the same question again. In act five, ask what would have happened if the old key had been removed first.

## Exercises

1. Add a third key, statements-v3, seal a message with it, and list which messages open if v1 and v2 are both removed.
2. Make a keystore with two keys of different purposes and use HKDF to make a third from one of them.
3. Check a keystore made by `keytool -genseckey` and see which round counts its file records.
4. Write a test that fails if a key text appears in any compiled class under `build/classes`.
