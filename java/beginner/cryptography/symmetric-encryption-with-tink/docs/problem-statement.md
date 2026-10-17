# Problem Statement

## The scenario

The online bank from the plain project wants to encrypt statement lines in several services, replace keys on a schedule without losing old data, and not rely on every developer remembering the rules about nonces and modes.

## The naive version

Use the JDK's AES-GCM directly in every service, each with its own nonce handling, key storage and message layout. It works until one service gets one of those details wrong.

## What this project must deliver

- An `Aead` built from a keyset, sealing and opening with one call each.
- A demonstration that Tink picks a new nonce every time, with the stored layout explained.
- A demonstration of refusal for a changed message, a moved message and a wrong key.
- Key rotation: add a primary key, keep old messages readable, and remove the old key.
- A keyset written encrypted under a master key, and read back.
- A statement of what Tink decides for the bank and what it leaves.
- The same output every run, checked against a golden output file.
