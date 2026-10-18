# Passkeys and WebAuthn, Explained

## The idea in one sentence

A passkey is a key pair made for one site: the site stores only the public key, and signs in by checking a fresh, single-use challenge signed together
with the site's own name and the page's real origin, so the key cannot be used anywhere else.

## The 5 acts

### 1. Registering a passkey: the bank keeps only a public key

The bank sends a challenge of 32 random bytes and its relying party id, bank.example. Ana's phone makes a new P-256 key pair for bank.example,
keeps the private key, and returns a credential id of 16 bytes and a public key of 91 bytes. The bank checks the challenge, the type, the origin
and the relying party id hash, and registers the passkey. What it stores is a credential id, the public key and a counter of 0: nothing that can
sign. An answer made with another key under Ana's credential id, as someone holding a copy of the bank's records might try, is refused: the
signature does not match.

### 2. Signing in: a fresh challenge, signed once

Ana touches her phone, which signs 37 bytes of authenticator data and the SHA-256 of the client data. The bank checks the challenge, the type, the
origin, the relying party id hash, the flags, the signature and the counter, and Ana is signed in. The same answer sent again is refused: its
challenge was single-use. An answer that arrives 301 seconds after its challenge is refused: challenges last five minutes. And an answer whose
user-verified flag was switched on after signing is refused, because the flags are inside the signed bytes.

### 3. A look-alike site gets nothing it can use

Ana opens https://bank-example.test, a copy of the bank's page. It asks for a passkey for its own name, bank-example.test, and gets nothing: the
phone has no passkey for that name. It passes on the bank's real challenge and asks for bank.example, and gets nothing: the browser will not use
bank.example for a page at bank-example.test. Even a browser that skipped that rule writes the page's real origin into the client data, and the
bank refuses it: the origin is not https://bank.example. The same page asking for Ana's password gets a password that works at the bank. A password
can be phished; a passkey cannot.

### 4. User verification for payments, and counters against copies

A payment of 900 with only a touch is refused: payments need user verification. With Ana's fingerprint as well, it is accepted. Ana's phone counts
its signatures, and the bank has now seen counter 6. An attacker copies the phone's key. The real phone signs in first, with counter 7. Then the copy
signs in with counter 7, which is not above 7, and is refused as a possible cloned authenticator; the passkey is suspended until Ana confirms it,
even for the real phone. A synced passkey, shared between Ana's devices, reports counter 0 every time and is accepted: a copy of it cannot be spotted
by its counter, so its protection is the account it syncs through.

### 5. The bill: recovery, lost devices and the move away from passwords

Ben registers 2 passkeys, his phone and a security key, and keeps 1 recovery code of 20 characters. His phone is lost, and his new laptop has no
passkey yet. The security key from his drawer signs him in. The recovery code works once and is refused the second time. Recovery is now the
weakest way in, so it needs the same care as the passkeys themselves. And Ana's password still works alongside her passkeys: the weakest way in
sets the level. Once Ana has two passkeys and the bank switches her password off, the password is refused. A real bank uses a maintained WebAuthn
library, which is the `-with-spring-security` version.

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
