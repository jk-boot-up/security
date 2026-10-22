# Session Guide — Passkeys with Spring Security

## Learning Objectives

By the end of the session you can:

- explain what the bank stores for a passkey and why it cannot sign
- explain why a look-alike site cannot use a passkey
- name what Spring Security's WebAuthn support does not check by default
- plan the loss of a device and the end of the password

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Registering a passkey: the bank keeps only a public key | 7 min |
| 0:17 | Act 2: Signing in: a fresh challenge, signed once | 7 min |
| 0:24 | Act 3: A look-alike site gets nothing it can use | 7 min |
| 0:31 | Act 4: User verification, and counters against copies | 7 min |
| 0:38 | Act 5: The bill: recovery, lost devices and the move away from passwords | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and list the three places a look-alike is stopped. In act four ask which of your own sign-ins would accept a clone. In act five ask what a customer does when the only passkey is on a lost phone.

## Exercises

1. Store the credentials in a database with the JDBC repositories that Spring Security provides.
2. Require user verification only for sign-ins that start a payment.
3. Add attestation verification and allow only security keys of one vendor.
4. Write the recovery path: a second passkey, a code, and a waiting period.
