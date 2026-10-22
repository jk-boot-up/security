# Problem Statement

## The scenario

An online bank wants customers to sign in with something that cannot be phished, and has to decide what the library leaves to the bank.

## The naive version

Add the passkey dependency and the configuration line, and assume that every check a bank would want is on.

## What this project must deliver

- Spring Security's WebAuthn endpoints in a real bank.
- An authenticator in software that builds real CBOR, COSE keys and signatures.
- The defaults, and a wrapper that requires user verification and detects clones.
- Lost devices and the end of the password.
