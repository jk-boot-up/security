# Session Guide — Digital Signatures with Bouncy Castle and OpenSSL

## Learning Objectives

By the end of the session you can:

- say what a CMS SignedData message contains
- explain the difference between attached and detached signatures
- explain why an ECDSA nonce must never repeat, and two ways to avoid the risk
- explain why a JWS verifier must choose the algorithm
- say what a valid signature does not prove

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A standard signed message: CMS SignedData | 7 min |
| 0:17 | Act 2: ECDSA: the nonce that must never repeat | 7 min |
| 0:24 | Act 3: EdDSA: Ed25519 and Ed448 | 7 min |
| 0:31 | Act 4: A compact signed token: JWS | 7 min |
| 0:38 | Act 5: Text armor, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the notary's stamp. Run the demo one act at a time. After act one, ask what the certificate in the message proves and what it cannot. After act two, ask why Ed25519 has no such risk. After act four, ask what the attacker gains from alg none. Finish with the bill: how would the bank prove who owns the key?

## Exercises

1. Add a second signer to the CMS message and verify each separately.
2. Sign with a P-384 key and SHA-384 and verify the result with `openssl cms -verify`.
3. Recover the private key from two signatures that share a nonce, using a different curve.
4. Add an `exp` claim to the JWS payload and make the verifier refuse an expired token on a simulated clock.
