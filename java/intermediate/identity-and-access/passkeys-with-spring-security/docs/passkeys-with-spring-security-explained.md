# Passkeys with Spring Security, Explained

## The idea in one sentence

A passkey proves that a customer holds a private key made for the bank's name, so it cannot be phished, and a bank that uses it still decides about user verification, copies, recovery and the password.

## The 5 acts

### 1. Registering a passkey: the bank keeps only a public key

Ana signs in with her password and asks for registration options (POST /webauthn/register/options). Spring Security sends a challenge of 32 random
bytes, the relying party (id bank.example, name The Bank), her user handle of 32 random bytes, the algorithms it accepts (-8 EdDSA, -7 ES256,
-257 RS256), user verification preferred and attestation none. Her phone makes a new P-256 key pair and posts the answer to /webauthn/register: a
credential id of 16 bytes, an attestation object with fmt none, and a COSE public key of 77 bytes. Decoded, the COSE key is five label = value
pairs: kty 2 (EC2), alg -7 (ES256), crv 1 (P-256), and the x and y coordinates of 32 bytes each. The bank checks the challenge, the type, the origin
and the relying party id hash (200) and stores a CredentialRecord: the credential id, the public key, a counter of 0 and the label, nothing that can
sign. An answer made with another key under Ana's credential id is refused with 401.

### 2. Signing in: a fresh challenge, signed once

POST /webauthn/authenticate/options gives a challenge of 32 random bytes, new on every call, the relying party id bank.example and user verification
preferred. Step one: the browser writes the client data with the type webauthn.get, the challenge and the origin. Step two: the phone writes 37
bytes of authenticator data: the SHA-256 of bank.example, flags 05 (user present and user verified) and a counter of 1. Step three: the signed bytes
are those 37 bytes followed by the 32-byte SHA-256 of the client data, 69 bytes. Step four: the phone signs them with ES256; the signature, about 70
bytes, differs on every run. POST /login/webauthn: Spring Security checks it all and answers 200, signed in as Ana with a passkey. The same answer
again, from another browser or from the browser that asked, an answer for another browser's challenge, and flags changed from 01 to 05 after signing
are all refused with 401.

### 3. A look-alike site gets nothing it can use

Ana opens https://bank-example.test, a copy of the bank's page. Request one asks for a passkey for its own name and gets nothing: the phone has no
passkey for bank-example.test. Request two passes on the bank's real challenge with the relying party id bank.example. The browser works out its rule:
is bank-example.test equal to bank.example? No. Does it end with .bank.example? No. So nothing comes back. Request three: a browser that skipped that
rule still writes the page's real origin into the signed client data, https://bank-example.test, which is not in the allowed origins the bank
configured ([https://bank.example]), and Spring Security refuses it with 401. The same page asking for Ana's password gets a password that works.

### 4. User verification, and counters against copies

Spring Security's defaults ask for user verification (preferred) but do not require it: a touch only, flags 01, is accepted. A phone that had reached
counter 3 and was restored from a backup signs with counter 1 and is accepted, and a copied key is accepted too: the counter is stored, never compared.
The hardened bank wraps Spring Security's relying party in HardenedRelyingParty and sets user verification to required: a touch only (flags 01) is
refused, a touch and a fingerprint (flags 05) accepted. The bank has seen counter 6. The real phone signs with 7: 7 is above 6, accepted. The copy
signs with 7: 7 is not above 7, refused, and the passkey is suspended, so the real phone with counter 8 is refused too. A synced passkey sets flags
1d (present, verified, backup eligible, backed up) and always reports counter 0, so a copy of it cannot be spotted by its counter.

### 5. The bill: recovery, lost devices and the move away from passwords

Ben registers 2 passkeys, his phone and a security key, each with a label. His phone is lost: his new laptop has no passkey for bank.example, and the
security key from his drawer signs him in. Signed in with the key, he sends DELETE /webauthn/register/ with the lost phone's credential id and gets
204; a thief who holds the phone and its fingerprint now gets 401. Recovery is now the weakest way in. Ana's old password, still switched on, is
accepted: the weakest way in sets the level. Once she has 2 passkeys, the bank's PasswordSwitchOffFilter refuses her password with 401, while Ben,
with no passkey, keeps his.

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
