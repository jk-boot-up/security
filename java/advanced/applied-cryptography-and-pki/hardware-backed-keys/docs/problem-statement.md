# Problem Statement

## The scenario

A bank signs payment files with one key and wants a thief who gets onto a server or a backup not to walk away with it.

## The naive version

Keep the key in a keystore file with a password next to the application.

## What this project must deliver

- A key generated on a PKCS#11 token.
- Signing from Java through SunPKCS11.
- Five attempts to export the key and their answers.
- Measured limits of a token: PIN, wrapping and a host that is already logged in.
