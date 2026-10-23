# Hardware-Backed Keys, Explained

## The idea in one sentence

A token stops the key from being copied, not from being used, so limit and watch how it is used.

## The 5 acts

### 1. The key ceremony

The token generates the pair with C_GenerateKeyPair and a template that makes the private half sensitive and not extractable, so it is also never extractable and always sensitive. A certificate signed by the token key is stored next to it, which is what makes the Java KeyStore list the key.

### 2. Five ways out

Reading the private exponent gives CKR_ATTRIBUTE_SENSITIVE, getEncoded() gives null, wrapping under the bank's key or an attacker's key gives CKR_KEY_UNEXTRACTABLE, and a PKCS12 export is refused. Meanwhile 5 of 5 signatures verify with the exported public key.

### 3. PINs and a file for contrast

Without a login a session sees 0 private keys, with one it sees 1. The token never answers CKR_PIN_LOCKED, so an application lockout of 3 tries is added, and the same right PIN given straight to the provider is still accepted. A stolen copy of the token directory takes 300 guesses with no lock and signs once the PIN is known; the key in a PKCS12 file falls to a dictionary at guess 6 of 8.

### 4. Rate, approvers and audit

At most 8 signatures in 60 seconds and two approvers other than the requester for files of 100,000.00 or more. Each decision is an entry in a hash chain: editing one entry or removing one breaks the chain.

### 5. A compromised host

The token is logged in for the whole process, so code that finds the provider signs 500 of 500 forged files, adds 0 audit entries and ignores the policy. After a logout it fails. A process that has learned the PIN signs too. A key made extractable for backup is wrapped, destroyed and restored, but an attacker wraps it under a key of their own and opens the blob in Java.

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
