# Session Guide — The TLS Handshake

## Learning Objectives

By the end of the session you can:

- say what a TLS handshake achieves
- say what a sniffer can and cannot see with TLS
- explain why a client checks a certificate and a name
- explain forward secrecy in one sentence
- say why old protocol versions are switched off

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A sniffer on the wire, without and with TLS | 7 min |
| 0:17 | Act 2: The handshake | 7 min |
| 0:24 | Act 3: Who is on the other end | 7 min |
| 0:31 | Act 4: Protecting the data | 7 min |
| 0:38 | Act 5: Settings, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the code word and the stranger. Run the demo one act at a time. After act one, ask what a thief learns from the capture of a TLS connection. After act two, ask why the first message cannot be encrypted.
In act three, ask what would happen if the client accepted every certificate. In act four, ask what an attacker could do to a record without the key.

## Exercises

1. Print the record types of the whole handshake and match them with the ClientHello, ServerHello and the encrypted records.
2. Make the client trust both certificates and see the impostor accepted.
3. Offer only TLS 1.3 on the client and a cipher suite that the server does not allow.
4. Swap two records on the wire and see what the server does.
