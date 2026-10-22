# Session Guide — Inspecting TLS Traffic with Wireshark

## Learning Objectives

By the end of the session you can:

- read a capture with and without a key log
- say what a key log contains and what it does not
- explain why one key log opens one connection
- explain what a leaked server key opens

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the wire shows | 7 min |
| 0:17 | Act 2: A key log: the secrets of one connection | 7 min |
| 0:24 | Act 3: The same capture, read with the key log | 7 min |
| 0:31 | Act 4: A key log opens its own connection and no other | 7 min |
| 0:38 | Act 5: The server's own key is a different matter | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and list what the watcher learns. In act three find what TLS 1.3 encrypts that TLS 1.2 does not. In act five find which recording stays closed.

## Exercises

1. Add a third login and a key log with only its server lines.
2. Capture a TLS 1.2 login with ECDHE and open it with a key log.
3. Put the key log under a mode of 600 and check who can read it.
4. Find the packet sizes in the capture and guess which one is the login.
