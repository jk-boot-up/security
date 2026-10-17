# Problem Statement

## The scenario

The online bank from the plain project wants to use a library for its one-time codes instead of its own HMAC code, and wants to know what that library
does and what is still its own job.

## The naive version

Assume the library handles two-factor login. It makes a code and checks it for a step, so the bank may believe that clock drift, replay and secrets are
handled. They are not.

## What this project must deliver

- The library's codes checked against the published values of RFC 4226 and RFC 6238.
- A bank class that adds a window and replay memory around the library's single-step check.
- A demonstration of other hashes and digit counts.
- The `otpauth://` link that a QR code carries.
- A statement, shown by the demo, of what the library leaves to the bank.
- The same output every run, checked against a golden output file.
