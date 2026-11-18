# Passkeys and WebAuthn, Explained

## The idea in one sentence

A passkey is a key pair made for one site: the site stores only the public key, and signs in by checking a fresh, single-use challenge signed together
with the site's own name and the page's real origin, so the key cannot be used anywhere else.

## The 5 acts

### 1. Registering a passkey: the bank keeps only a public key

The request: a challenge of 32 random bytes, sent as 43 base64url characters, and the relying party id bank.example. The browser, not the page,
writes the client data: the type webauthn.create, the challenge and the origin https://bank.example. The response: Ana's phone makes a new P-256
key pair for bank.example, keeps the private key, and returns a credential id of 16 bytes and a public key of 91 bytes. The bank checks the
challenge, the type, the origin and the relying party id hash, and stores a credential id, the public key and a counter of 0: nothing that can
sign. An answer made with another key under Ana's credential id, as someone holding a copy of the bank's records might try, is refused: the
signature does not match.

### 2. Signing in: a fresh challenge, signed once

A fresh 32-byte challenge. Step one: the browser writes 133 bytes of client data with the type webauthn.get, the challenge and the origin.
Step two: the SHA-256 of those bytes, 32 bytes. Step three: the phone writes 37 bytes of authenticator data: the SHA-256 of bank.example, a flags
byte 01 (user present, not verified) and a counter of 1. Step four: the signed bytes are the authenticator data followed by the client data hash,
37 + 32 = 69 bytes. Step five: the phone signs them with its private key (ES256), a 71-byte signature. The bank checks the signature with the
stored public key over the same 69 bytes, then every other field, and Ana is signed in. The same answer sent again is refused (its challenge was
single-use), an answer 301 seconds after its challenge is refused (the limit is 300), and an answer whose flags were changed from 01 to 05 after
signing is refused: the flags are inside the signed bytes.

### 3. A look-alike site gets nothing it can use

Ana opens https://bank-example.test, a copy of the bank's page. Request one asks for a passkey for its own name, bank-example.test, and gets
nothing: the phone has no passkey for that name. Request two passes on the bank's real challenge and asks for bank.example. The browser works out
its rule: is bank-example.test equal to bank.example? No. Does it end with .bank.example? No. So bank.example is not this page's site, and nothing
comes back. Request three: even a browser that skipped that rule writes the page's real origin, https://bank-example.test, into the signed client
data, and the bank, which expects https://bank.example, refuses it. The same page asking for Ana's password gets a password that works at the bank.

### 4. User verification for payments, and counters against copies

A payment of 900 asks for user verification. With only a touch, the flags byte is 01 (user present) and the payment is refused. With a fingerprint
as well, the flags byte is 05 (01 for present plus 04 for verified) and it is accepted. The bank has now seen counter 6. An attacker copies the
phone's key. The real phone signs with counter 7: 7 is above 6, accepted. The copy signs with counter 7: 7 is not above 7, refused as a possible
cloned key, and the passkey is suspended, so even the real phone with counter 8 is refused until Ana confirms it. A synced passkey, shared between
Ana's devices, reports counter 0 every time and is accepted: a copy of it cannot be spotted by its counter.

### 5. The bill: recovery, lost devices and the move away from passwords

Ben registers 2 passkeys, his phone and a security key, and keeps 1 recovery code of 20 characters, of which the bank keeps only the SHA-256.
His phone is lost, and his new laptop has no passkey for bank.example. The security key from his drawer signs him in. The recovery code works once
and is refused the second time. Recovery is now the weakest way in, so it needs the same care as the passkeys themselves. Ana's old password, still
switched on, is accepted: the weakest way in sets the level. Once Ana has two passkeys and the bank switches her password off, it is refused. A real
bank uses a maintained WebAuthn library, which is the `-with-spring-security` version.

## The verdict

Offer passkeys as the main way in, with a maintained library making the checks. Require user verification for payments and changes to the account.
Encourage two passkeys per customer, issue one-time recovery codes, guard the recovery flow, and switch passwords off once they are no longer needed.

## How to recognise this in code you did not write

- `navigator.credentials.create` and `navigator.credentials.get` in a site's JavaScript.
- A server that stores `credentialId`, `publicKey` and `signCount` per user.
- Library settings named `rpId`, `origins` and `userVerification`.
- Account settings pages listing several passkeys by device name.

## Where you have already met this

- "Sign in with a passkey" buttons, and a fingerprint or face prompt instead of a password field.
- Security keys that plug into a USB port and must be touched.
- Passkeys that appear on a new phone because they were synced through the phone maker's account.
- Bank apps that ask for a fingerprint again before a payment.
