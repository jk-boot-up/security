# Post-Quantum Cryptography, Explained

## The idea in one sentence

Recorded traffic can be opened later, so move key exchange first, move it as a hybrid, and write down where the old algorithms are.

## The 5 acts

### 1. What has to cross the network

The input: one real key pair, ciphertext or signature of each algorithm, made in the JDK, with public keys in their X.509 form.
Key exchange: X25519 44 and 44 bytes, ECDH P-256 91 and 91, RSA-3072 key transport 422 and 384, ML-KEM-512 822 and 768, ML-KEM-768 1206 and 1088, ML-KEM-1024 1590 and 1568, each with a 32-byte secret. Worked out: 1206 / 44 = 27.4 times, 1088 / 44 = 24.7 times, and a hybrid sends 44 + 1206 = 1250 bytes one way and 44 + 1088 = 1132 back.
Signatures: Ed25519 44 and 64, ECDSA P-256 91 and 64, RSA-3072 422 and 384, ML-DSA-44 1334 and 2420, ML-DSA-65 1974 and 3309, ML-DSA-87 2614 and 4627. One TLS handshake carries 2 certificate keys and 3 signatures: 2 x 91 + 3 x 64 = 374 bytes for ECDSA P-256, 2 x 1974 + 3 x 3309 = 13875 for ML-DSA-65, 37.1 times more.

### 2. ML-KEM in the JDK

The input: the bank's ML-KEM-768 public key, 1206 bytes in X.509 form. The sender encapsulates to it and gets a 1088-byte ciphertext, which it sends, and a 32-byte shared key, which it keeps. The bank decapsulates with its private key and gets the same 32 bytes. A second encapsulation gives a new ciphertext and a new key.
A tampered ciphertext: the message "card 4111 declined" is sealed with the shared key, one bit of the ciphertext is flipped, and decapsulation raises no error. The bank gets a 32-byte key that differs from the sender's, and its first use, opening the message, fails the AES-GCM tag check. The untouched ciphertext opens the message. Only a ciphertext of the wrong length, 10 bytes, raises DecapsulateException.
Bouncy Castle 1.86, a second implementation: a public key re-encoded by the JDK is byte for byte the same, and encapsulation in one library and decapsulation in the other give the same key, both ways.

### 3. ML-DSA in the JDK

The input: the statement "statement 2031-04 account 4417: closing balance 1200.00", signed with an ML-DSA-65 key; the public key is 1974 bytes and the signature 3309.
The checks: as signed, it verifies; with one bit changed it does not; with another bank's key it does not; signed a second time, the signature differs (ML-DSA mixes in fresh randomness, called hedged signing) and also verifies; an ML-DSA-44 signature checked with an ML-DSA-65 key does not verify.
Bouncy Castle 1.86: the JDK signs and Bouncy Castle verifies, Bouncy Castle signs and the JDK verifies; the JDK does not read Bouncy Castle's private key encoding, so private keys stay in the library that made them.

### 4. A hybrid key exchange: X25519 and ML-KEM-768

What crosses the network: the client sends an X25519 key of 44 bytes and an ML-KEM key of 1206, 1250 bytes; the server sends an X25519 key of 44 and a ciphertext of 1088, 1132 bytes.
How the key is made, worked by hand: the secrets are X25519 32 + ML-KEM 32 = 64 bytes; the context is the label (27 bytes) and every public value, 27 + 44 + 1206 + 44 + 1088 = 2409 bytes; extract is HMAC with 32 zero bytes as the key over the secrets, 32 bytes; expand is HMAC with that over the context and the byte 01, 32 bytes. By hand equals the JDK's KDF, the client's key equals the server's, and it differs from a key of X25519 alone.
A recording attacked years later: with X25519 broken the attacker recovers its secret, but a hybrid key rebuilt without the ML-KEM secret does not match; an X25519-only key would be rebuilt. With ML-KEM broken, a key rebuilt without the X25519 secret does not match. With one ciphertext byte missing from the context, the key does not match.

### 5. Planning the migration

Mosca's inequality, with made-up inputs and not a prediction: data is exposed when x + y > z, where x is how many years it must stay secret, y how many years the migration takes, and z how many years until a quantum computer breaks the old algorithms.
The bank's data: card authorisation 1 + 2 = 3, online banking session 0 + 2 = 2, statement archive 10 + 3 = 13, loan contracts and identity documents 30 + 4 = 34, settlement file 7 + 5 = 12. For z = 8, 15 and 25 the result is the years left to start, or how late it already is: at z = 8 the statement archive is 5 years late, the loan contracts 26 and the settlement file 4, so 3 of 5 are exposed; at 15 and 25 only the loan contracts, 1 of 5.
The inventory reads 7 of the bank's source files as text: online-banking/TlsSetup (X25519), interbank/SettlementChannel (ECDH, AES) and archive/ArchiveKeyWrap (RSA key transport) are key exchange, first, because a recording opens later; statements/StatementSigner (RSA), mobile/DeviceBinding (ECDSA) and identity/LoginTokens (Ed25519, HMAC) are signatures, second; payments/CardVault (AES, HMAC) is symmetric only and stays.

## The verdict

Inventory the public-key algorithms, estimate how long each kind of data must stay secret, move key establishment to a hybrid of X25519 and ML-KEM first, plan signatures after, and keep the algorithm a configuration choice.

## How to recognise this in code you did not write

- A plan that starts when the quantum computer is announced.
- No list of where RSA and elliptic curves are used.
- A new algorithm used alone in production on day one.
- Certificates whose size nobody checked against the handshake budget.

## Where you have already met this

- Waiting for a quantum computer to exist before planning.
- Replacing RSA with a larger RSA key.
- A migration with no list of where the old algorithms are.
- A new algorithm used alone on day one.
