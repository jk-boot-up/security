# Session Guide — TLS Attacks and Downgrades

## Learning Objectives

By the end of the session you can:

- read a testssl.sh report and count it against a known truth
- explain a protocol downgrade and what stops it
- tell why static RSA loses forward secrecy and ECDHE does not
- list what TLS 1.3 removed and why

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The weak server | 7 min |
| 0:17 | Act 2: Probes and testssl.sh | 7 min |
| 0:24 | Act 3: The downgrade dance | 7 min |
| 0:31 | Act 4: The leaked key | 7 min |
| 0:38 | Act 5: What TLS 1.3 removed | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and find the two weaknesses that testssl.sh shows only in its cipher table. Then change the fallback client to send the signal and see the server's answer.

## Exercises

1. Add a seventh planted weakness, a certificate that is signed with SHA-1, and see who finds it.
2. Make the middlebox break TLS 1.2 only and see which version the client ends on.
3. Run the decryption on a CBC suite (AES128-SHA).
4. Start the strong server with a 2048-bit DH group and scan it again.
