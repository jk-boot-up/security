# Hardware-Backed Keys, Explained

## The idea in one sentence

A token stops the key from being copied, not from being used, so limit and watch how it is used.

## The 6 acts

### 1. The key ceremony

The input is the token and the template: SoftHSM2 2.7.0, used through Java's SunPKCS11 provider and raw PKCS#11 calls, initialised with a security officer PIN and a user PIN. The bank calls C_GenerateKeyPair for an RSA 2048 pair made inside the token, with CKA_SENSITIVE true and CKA_EXTRACTABLE false.
The result is read back from the token: sensitive yes, extractable no, never extractable yes, always sensitive yes. A certificate signed by the token key verifies, and its public key equals the one exported from the token.

### 2. Five ways out, and signing

The input is five ways to get the private key out: read the private exponent with C_GetAttributeValue, ask Java for getEncoded(), wrap it under the bank's wrapping key with C_WrapKey, wrap it under a key the attacker can read, and export it into a PKCS12 file.
The result: CKR_ATTRIBUTE_SENSITIVE, null (getFormat() is null too), CKR_KEY_UNEXTRACTABLE, CKR_KEY_UNEXTRACTABLE and refused. Private key obtained: 0 of 5.
Signing still works. One payment file of 140 bytes (FILE PAY-2026-001, four PAY lines, TOTAL 1250000) goes into the token with SHA256withRSA. The signature is 256 bytes, because an RSA 2048 signature is 2048 bits / 8. All 5 files verify with the exported public key; the same signatures on files with one bit changed: 0 of 5.

### 3. The PIN, a key in a file, and a copied token

Without the PIN the KeyStore is refused and a session sees 0 private keys; after login it sees 1. Twelve wrong PINs are refused 12 of 12 with CKR_PIN_INCORRECT, the token never locks (SoftHSM2 has no lockout) and the right PIN is then accepted.
The bank's own lockout of 3 tries answers refused, refused, refused, locked: the token is asked 3 of 4 times. But the same right PIN given straight to the provider is accepted, and an officer reset lets it in again.
The same kind of key in a PKCS12 file, copied by an attacker: the password is found at guess 6 of 8, offline, with no limit and no log, and a forged file signed on the thief's machine verifies. A copy of the token directory on another machine takes 300 wrong guesses with 0 hits and no lock; each guess takes under 86.4 ms, so all 1,000,000 six-digit PINs take under a day (1,000,000 x 86.4 ms = 86,400 s). With the PIN, the copy signs and the signature verifies.

### 4. Rate, approvers and audit

The input is the bank's rules and 14 requests. Rules: at most 8 signatures in 60 seconds, and 2 people other than the requester for files of 100,000.00 or more. Requests: alice sends one every 4 seconds, 1,500.00 each, except numbers 4, 9 and 13 of 250,000.00, approved by carol and dave, by carol alone, and by alice and bob.
The result, request by request: 0 to 7 signed, 8 refused for the rate, 9 refused for approvers, 10 to 12 refused for the rate, 13 refused for approvers (alice cannot approve her own file). 8 + 4 + 2 = 14, and 8 of 8 signatures verify.
How an audit entry is made: hash = SHA-256(seq|event|file|content hash|previous). Entry 1 has previous "genesis" and hash eeef9a22d1f6...; entry 2 carries eeef9a22d1f6 as its previous. The 14 entries verify; with entry 4 edited or entry 6 removed, they do not.

### 5. A compromised host

The input is five things an attacker on the bank's host tries: another process opens the token with no PIN; it asks the bank's service to sign 99,999,999.00 with no approvers; code inside the application's process signs 500 forged files with no PIN; the same code tries again after the application logs out; and another process that has learned the PIN signs.
The result: 1, login refused and 0 private keys seen; 2, refused, needs two approvers; 3, 500 of 500 signed, all verify with the bank's public key, 0 audit entries added and the chain still verifies; 4, after the logout neither an old key reference nor a fresh KeyStore signs; 5, the process with the PIN signs and the signature verifies.

### 6. A key that can be backed up, and the limits

The input is a release-signing key, EC P-256, made on the token with CKA_EXTRACTABLE true, and a sensitive AES token key to wrap it with AES key wrap with padding.
The backup cycle: wrap gives CKR_OK and a blob of 80 bytes; after the key is destroyed, signing gives CKR_OBJECT_HANDLE_INVALID; unwrapping the blob gives CKR_OK, and the restored key signs and verifies with the original public key.
An attacker with a logged-in session wraps the same kind of key under an AES key they can read (CKR_OK) and opens it in Java: 67 key bytes, padded to 72, + 8 = 80, which is the blob size. A forged release signature verifies. When the wrapping key is lost with the token, the blob restores nothing (CKR_UNWRAPPING_KEY_HANDLE_INVALID).
What the token protects: ways to copy a key out that worked were 0 of 5 for the non-extractable key, 1 of 1 for the extractable one and 1 of 1 for the key in a file. What it does not: use while logged in, by anyone in the process, and the rate, approvers and audit kept in the application.

## The verdict

Generate signing keys on a token as sensitive and not extractable, log in only for the time you sign, keep rate limits, approvers and audit outside the process that holds the login, and treat a wrappable key as exported.

## How to recognise this in code you did not write

- A .p12 file and its password in a deployment.
- A build server that can sign any release.
- A backup of a key on a second machine.
- An HSM with one shared PIN for every application.

## Where you have already met this

- A signing key in a keystore file in the repository.
- A key password in an environment variable on the build server.
- A key copied to a second server as a backup.
- An HSM that the whole application server can use without limits.
