# Problem Statement

## The scenario

An online bank wants to send a customer encrypted statements. The bank and the customer have never met, share no secret, and communicate over a network on which an eavesdropper sees everything.

## The naive version

Send the encryption key first, over the same network. The eavesdropper copies it, and the encryption is worthless.

## What this project must deliver

- A demonstration that a key sent in the clear is a key Eve has.
- RSA key pairs with OAEP padding, with their limits and sizes.
- Hybrid encryption of a statement far longer than RSA can lock.
- X25519 key agreement with HKDF, checked against the RFC test values.
- A man in the middle who swaps a public key, to show what these methods leave open.
- The same facts printed on every run, though every key is random.
