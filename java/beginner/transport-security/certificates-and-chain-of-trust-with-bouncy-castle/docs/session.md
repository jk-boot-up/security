# Session Guide — Certificates and Chain of Trust with Bouncy Castle and OpenSSL

## Learning Objectives

By the end of the session you can:

- name the extensions that set a certificate's role
- explain what a name constraint protects against
- explain how a revocation list works and when it fails
- describe what a signing request proves and why the private key stays home

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A chain built with Bouncy Castle | 7 min |
| 0:17 | Act 2: A limit the authority carries in its own certificate | 7 min |
| 0:24 | Act 3: A revocation list signed by the issuer | 7 min |
| 0:31 | Act 4: Asking for a certificate without handing over the private key | 7 min |
| 0:38 | Act 5: A stolen authority key, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the passport office. Run the demo one act at a time. After act two, ask what an attacker with the intermediate's key could still do. After act three, ask what a browser should do when it cannot fetch a list. In act four, ask why the request is signed.
Then run the integration tests, and compare OpenSSL's text output with the extensions in act one.

## Exercises

1. Remove the name constraint from the intermediate and see which act changes.
2. Add a second server certificate and revoke only that one.
3. Run `openssl x509 -text` on a certificate the demo writes and find the key identifiers.
4. Change the validation date to after the server certificate ends and read the reason.
