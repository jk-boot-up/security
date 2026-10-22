# Passkeys with Spring Security, Explained

## The idea in one sentence

A passkey proves that a customer holds a private key made for the bank's name, so it cannot be phished, and a bank that uses it still decides about user verification, copies, recovery and the password.

## The 5 acts

### 1. Registering a passkey: the bank keeps only a public key

Ana signs in with her password and adds a passkey: Spring Security's WebAuthn support sends a challenge of 32 random bytes, the relying party id bank.example and her user handle of 32 random bytes. Ana's phone makes a new P-256 key pair for bank.example, keeps the private key, and returns a credential id of 16 bytes and an attestation object that says fmt none. The bank checks the challenge, the type, the origin and the relying party id hash, and accepts. It stores a credential id of 16 bytes, a COSE public key of 77 bytes, a counter of 0 and the label, and nothing that can sign. An answer made with another key under ana's credential id is refused.

### 2. Signing in: a fresh challenge, signed once

Every sign-in gets a new challenge of 32 random bytes. Ana touches her phone and uses her fingerprint; it signs 37 bytes of authenticator data and the SHA-256 of the client data. The bank checks the challenge, type, origin, relying party id hash, flags and signature: accepted, signed in as ana with a passkey. The same answer sent again from another browser is refused, and sent again to the browser that asked for the challenge it is refused because the challenge was used. An answer for a challenge that was given to another browser is refused. The same kind of answer with the user-verified flag switched on after signing is refused: the signature does not match.

### 3. A look-alike site gets nothing it can use

Ana opens https://bank-example.test, a copy of the bank's page. It asks for a passkey for its own name, bank-example.test: nothing, the authenticator has no passkey for that name. It passes on the bank's real challenge and asks for bank.example: nothing, the browser will not use bank.example for a page at that address. Even a browser that skipped that rule writes the page's real origin into the client data, and the bank checks it: refused. The same page asking for ana's password, which she types in, gets a password that works at the bank: accepted. A password can be phished, a passkey cannot.

### 4. User verification, and counters against copies

With Spring Security's defaults, a sign-in with only a touch is accepted: user verification is asked for, not required. The phone has signed in 3 times and is restored from a backup, so its counter goes back to 1: accepted. An attacker's copy of the key signing with the same counter as the real phone is accepted: a clone is not noticed. With the hardened bank, user verification required and the counter compared: a sign-in with only a touch is refused, with her fingerprint as well it is accepted. The bank has now seen counter 6. An attacker copies the phone's key; the real phone signs in first and is accepted; then the copy signs in: refused, counter 7 is not above 7, possible cloned authenticator. The passkey is now suspended until ana confirms it, even for the real phone. A synced passkey reports counter 0 and is accepted twice, so a copy cannot be spotted by its counter.

### 5. The bill: recovery, lost devices and the move away from passwords

Ben registers 2 passkeys, his phone and a security key. His phone is lost, and his new laptop has no passkey yet: nothing. The security key from his drawer is accepted. Signed in with the key, ben removes the lost phone's passkey, 204, and a thief who holds the phone and its fingerprint is refused. Recovery is now the weakest way in, so it needs the same care as the passkeys themselves. Ana's password still works alongside her passkeys: the weakest way in sets the level. Once ana has two passkeys and the bank switches her password off, the password is refused, while a customer with no passkey keeps it.

## The verdict

Use a maintained WebAuthn library, require user verification, compare the counter and suspend a passkey that goes backwards, treat synced passkeys as the account they sync through, offer two passkeys before switching passwords off, and design recovery as carefully as sign-in.

## How to recognise this in code you did not write

- `navigator.credentials.create` and `get` in front-end code.
- `http.webAuthn()` in a Spring Security configuration.
- A `PublicKeyCredentialRequestOptions` with `userVerification: preferred`.
- An `rpId` that is a registrable domain.

## Where you have already met this

- "Sign in with a passkey" and a fingerprint prompt.
- `navigator.credentials.create` and `navigator.credentials.get` in front-end code.
- `http.webAuthn()` in a Spring Security configuration.
- A security key (YubiKey) on a keyring.
