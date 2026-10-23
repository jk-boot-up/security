# Session Guide — Certificate Lifecycle

## Learning Objectives

By the end of the session you can:

- read the fields of a certificate that a client checks
- explain hard fail and soft fail for revocation
- compare a CRL, an OCSP response and a staple
- renew short-lived certificates before they expire
- monitor a fleet for expiry

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Issue and read | 7 min |
| 0:17 | Act 2: Revocation and a dead responder | 7 min |
| 0:24 | Act 3: Stapling | 7 min |
| 0:31 | Act 4: Short lives and rotation | 7 min |
| 0:38 | Act 5: The missed renewal | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read the revocation table first. Then find the line that shows the revoked certificate accepted and say which client policy and which failure allowed it.

## Exercises

1. Change the lifetime to 9 seconds and predict the number of renewals in 30 seconds.
2. Add a must-staple extension to the certificate and make the client refuse a server with no staple.
3. Make the monitor alert on the fleet by owner.
4. Serve the CRL over HTTP and let the JDK fetch it.
