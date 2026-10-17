# Digital Signatures with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

A digital signature in a standard format, such as CMS or JWS, can be checked by other programs, and it is only as safe as its nonce, the verifier's choice of algorithm and the certificate that says who owns the key.

## The 5 acts

### 1. A standard signed message: CMS SignedData

The bank signs a statement with its ECDSA key and puts its certificate in the message. Attached, the statement travels inside and verifies with the certificate. Detached, the statement travels separately and verifies when it is supplied.
A changed statement is refused. One bit changed inside an attached message is refused. Somebody else's certificate refuses it. The signing time inside, 2026-03-01T09:30:00Z, is covered by the signature but it is the signer's word.

### 2. ECDSA: the nonce that must never repeat

Deterministic signing of the same message twice gives the same signature, and it matches the value published in RFC 6979 appendix A.2.5. Randomized signing gives different signatures each time, and both verify.
Two signatures made with the same nonce let anyone compute the private key. Never reuse a nonce: deterministic signing removes the risk, and so does Ed25519.

### 3. EdDSA: Ed25519 and Ed448

An Ed25519 signature is 64 bytes and matches RFC 8032. An Ed448 signature is 114 bytes and matches RFC 8032. Signing again gives the same bytes, with no random number involved. Both verify, and one flipped bit in an Ed25519 signature is refused.

### 4. A compact signed token: JWS

Signing the RFC 8037 example gives exactly the token in the RFC, and it verifies. The detached form has an empty middle part and verifies when the payload is supplied. A token that claims alg none and has no signature is refused,
and so is the same signature with a different payload. The verifier decides the algorithm, not the token: that is what stops alg none and key confusion.

### 5. Text armor, and the bill

A signed message as PEM text begins with BEGIN CMS and reads back to the same bytes. Then the bill: a valid signature proves that the key signed, not who owns the key, which needs a chain of certificates. The signing time is the signer's claim: a trusted time
needs a timestamp authority (RFC 3161), described and not run. The private key still needs a home, which is the next project. OpenSSL checks the same files in the integration tests.

## The verdict

Use a standard format, deterministic signatures (Ed25519 or deterministic ECDSA), a verifier that fixes the expected algorithm, and a certificate chain that says who owns the key. Add a trusted timestamp when the time matters, and protect the private key.

## How to recognise this in code you did not write

- `openssl cms -sign` and `jarsigner` in build scripts.
- Signed PDFs and S/MIME mail.
- JWT libraries that ask you to pass the allowed algorithms.
- Reports of ECDSA keys recovered from bad random number generators.

## Where you have already met this

- Signed PDFs and S/MIME email, which are CMS SignedData.
- Java's `jarsigner` and Android's APK signing.
- JWT and JWS tokens in web logins, and `alg: none` in vulnerability reports.
- `openssl dgst -sign` and `openssl cms -sign` in scripts.
