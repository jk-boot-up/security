# Problem Statement

## The scenario

A developer needs to see the contents of an encrypted login for debugging, and the security team needs to know what that costs and what an attacker with recorded traffic could do.

## The naive version

Capture the traffic and ask for the server's private key, or set SSLKEYLOGFILE everywhere, including production, so that anything can be debugged later.

## What this project must deliver

- A TLS 1.3 style key schedule with real HKDF labels and X25519, and AES-GCM records.
- Captures of ephemeral and static-RSA connections, and a decryptor for each kind of key material.
- The NSS key log format, and a check that refuses the variable in production.
- A demonstration of forward secrecy against a leaked long-term key.
