# Problem Statement

## The scenario

An online bank's customers sign in with passwords and one-time codes, and both can be typed into a convincing copy of the bank's page. The bank
wants sign-in that a look-alike page cannot capture, without storing anything that would let a thief sign in.

## The naive version

Add another secret: a longer password, a security question, a code by text message. Each is still something a person can be talked into
entering on the wrong site, and each is stored somewhere a thief can copy.

## What this project must deliver

- Registration that stores only a public key, a credential id and a counter.
- Sign-in with single-use, five-minute challenges, checked in a fixed order.
- The browser's rule that a page may only ask for its own site's passkeys, and the origin written into the signed client data.
- User verification for payments, and a counter that catches a copied device-bound key.
- Recovery with a second passkey and one-time codes, and switching passwords off.
