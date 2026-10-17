# Dependencies

This project adds no Java library. It uses one command-line tool, and only for the integration tests.

## What OpenSSL is

a widely used cryptography toolkit and command-line program. The tests run `openssl s_server` and `openssl s_client` as the other end of a real TLS connection. They skip themselves if no OpenSSL 3 is found.

## Why this project uses them

A client and server that agree with each other may share the same mistake. A different implementation, written by other people, is the cheapest independent check that the setup follows the standard.

## What to install

A JDK for the demo and the ordinary tests. For the integration tests, OpenSSL 3 on the PATH or in Homebrew's openssl@3 folder. The system `openssl` on macOS is LibreSSL and is not used. The versions:

| Tool | Version |
| --- | --- |
| OpenSSL (integration tests) | 3.6.3 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The integration tests need OpenSSL 3 installed.
- Output formats of `openssl s_client -brief` can change between versions, so the tests look for a few stable phrases.
