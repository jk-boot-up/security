# Session Guide — Post-Quantum Cryptography

## Learning Objectives

By the end of the session you can:

- use ML-KEM and ML-DSA through java.security and javax.crypto
- explain why a hybrid exchange keeps today's security while trusting the new algorithm gradually
- read Mosca's inequality and say which data is exposed first
- list where an organisation uses algorithms a quantum computer breaks

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the new algorithms cost | 7 min |
| 0:17 | Act 2: ML-KEM and implicit rejection | 7 min |
| 0:24 | Act 3: ML-DSA and a hybrid exchange | 7 min |
| 0:31 | Act 4: Harvest now, decrypt later | 7 min |
| 0:38 | Act 5: Inventory and two implementations | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read the size table, the implicit rejection lines and the Mosca table. Change one input of the inequality and see which data class changes sides.

## Exercises

1. Add a data class of your own to the Mosca table.
2. Replace X25519 with ECDH P-256 in the hybrid exchange.
3. Count the algorithms in the advanced projects too.
4. Add ML-KEM-1024 to the hybrid exchange and compare the bytes.
