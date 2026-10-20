# Session Guide — Inspecting TLS Traffic with a Key Log

## Learning Objectives

By the end of the session you can:

- say what a packet capture of TLS 1.3 reveals and what it hides
- describe the key log format and what a line allows
- explain forward secrecy with a leaked long-term key
- list the controls that keep key logs out of production

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the wire shows | 7 min |
| 0:17 | Act 2: Writing a key log | 7 min |
| 0:24 | Act 3: Reading the capture with the key log | 7 min |
| 0:31 | Act 4: A key log opens its own connections and no others | 7 min |
| 0:38 | Act 5: Why the server's own key is a different matter | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask what an observer learns from the sizes alone. In act four, ask what a developer would have to collect to debug a long session. In act five, ask which of the two connections an attacker with a year-old recording would choose to attack, and why.

## Exercises

1. Add a second record to each direction and check that sequence numbers matter when records are reordered.
2. Add a TLS 1.3 key update that changes the traffic secret after N records, and extend the key log with a second label.
3. Make the decryptor report which direction it could not open and why.
4. Write a check that fails a startup health test when SSLKEYLOGFILE is present in the process environment of a production container.
