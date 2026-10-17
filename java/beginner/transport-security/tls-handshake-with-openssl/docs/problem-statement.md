# Problem Statement

## The scenario

An online store has set up a TLS server and a client. Both are written in Java and agree with each other, but they could both be wrong in the same way.

## The naive version

Test the Java client against the Java server and conclude that TLS works.

## What this project must deliver

- A real TLS server and client over sockets, with names and trust checked.
- The certificate and key written as PEM files that openssl reads.
- A demonstration of an impostor, a wrong name and old versions being refused.
- Integration tests in which openssl is the client and the server.
- A demo that prints the same output on every run, though it uses real sockets.
