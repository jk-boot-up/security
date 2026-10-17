# Problem Statement

## The scenario

The online bank must store customers' keys in files, issue them certificates, and send them encrypted statements in a format their own email program and other tools can read.

## The naive version

Invent a format: the key and the encrypted data in a custom file layout. It works for the bank's own code, and nothing else can read it, so every partner needs custom code too.

## What this project must deliver

- Keys written and read as PEM, with a password-protected private key.
- A self-signed X.509 certificate with fixed dates.
- A CMS envelope that uses AES-256 for the data and RSA-OAEP for the key.
- An independent check: the openssl command-line tool opens the envelope.
- A demonstration that the easy call picks the older padding.
- Unit tests that need nothing installed, and interop tests that call openssl.
