# Session Guide — The TLS Handshake with OpenSSL

## Learning Objectives

By the end of the session you can:

- explain why a second implementation is a stronger check
- read the output of `openssl s_client -brief`
- say what a PEM file is
- name two things a TLS client must verify besides encryption

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A real TLS server and client on this machine | 7 min |
| 0:17 | Act 2: Giving the certificate and key to other programs | 7 min |
| 0:24 | Act 3: Who is on the other end | 7 min |
| 0:31 | Act 4: Versions: what each side offers | 7 min |
| 0:38 | Act 5: Checking from the outside, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the map analogy. Run the demo one act at a time. Then run `./gradlew integrationTest` and read one openssl test next to the matching act. Ask what would happen if the openssl client were run without the option to return an error.

## Exercises

1. Run `openssl s_client -connect` against the Java server while it is running in a test, and read the whole output.
2. Add a test in which openssl serves the impostor certificate and the Java client must refuse it.
3. Add a server that requires a client certificate and check it with `openssl s_client -cert`.
4. Change the host name in the openssl command and read the exact error.
