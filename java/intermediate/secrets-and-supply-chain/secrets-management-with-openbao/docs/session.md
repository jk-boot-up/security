# Session Guide — Secrets Management with OpenBao

## Learning Objectives

By the end of the session you can:

- explain how an application proves who it is to a secrets server
- write a policy that names what an application may read
- lease a database login and say when it ends
- describe what the store does when the secrets server is sealed

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A secrets server, and who may ask | 7 min |
| 0:17 | Act 2: Secrets that stay still: versions, rotation and the audit trail | 7 min |
| 0:24 | Act 3: Database logins that are leased | 7 min |
| 0:31 | Act 4: Rotation, and the blast radius of a leak | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and read the lease durations. In act four ask what a thief gets from a leased login and from a static password. In act five ask what the store should do while OpenBao is sealed.

## Exercises

1. Let the application renew its own lease in a loop and log every renewal.
2. Wrap the secret id in a response-wrapped token so that only the application can unwrap it.
3. Give reports a read-only login that expires in five minutes, and a policy to match.
4. Run OpenBao with real storage and an auto-unseal, and write down the recovery steps.
