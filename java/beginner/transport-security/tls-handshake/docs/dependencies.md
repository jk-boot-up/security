# Dependencies

This project uses no library beyond the JDK. Its test certificates were made once with the JDK's `keytool`.

## What javax.net.ssl.SSLEngine is

the JDK's TLS engine without any sockets: it turns application data into records and back, and it runs the handshake when the caller passes it bytes.

## What keytool is

the JDK's tool for keystores. It made `shop.p12` and `impostor.p12` with an elliptic-curve key, the name shop.example.test, and a validity of one hundred years.

## Why this project uses them

Using the engine directly, with nothing but memory between the two sides, shows exactly what crosses the wire and keeps the demo exact and fast.

## What to install

A JDK is all that is needed. The versions are pinned where Gradle controls them:

| Tool | Version |
| --- | --- |
| Java (includes TLS 1.3) | 25 |
| JUnit | 6.1.3 |
| Keystore format | PKCS12 |
| Test key | EC P-256, 100-year certificate |

## What it costs

- SSLEngine is a low-level API: the caller moves every byte and must handle buffer sizes and the handshake loop.
- The test keys are public in the repository and protect nothing.
