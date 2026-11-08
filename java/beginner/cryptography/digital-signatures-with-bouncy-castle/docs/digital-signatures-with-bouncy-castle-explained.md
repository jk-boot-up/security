# Digital Signatures with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

A digital signature in a standard format, such as CMS or JWS, can be checked by other programs, and it is only as safe as its nonce, the verifier's choice of algorithm and the certificate that says who owns the key.

## The 5 acts

### 1. A standard signed message: CMS SignedData

Before signing, the bank has the statement, its SHA-256 digest (6e158e90 ... 324fb981) and the signing time. After signing, the CMS message holds the signer's certificate, the signed attributes (the same digest and the time) and an ECDSA signature over those attributes.
Attached, the statement travels inside; detached, it travels separately. Both verify with the certificate. A changed balance, one changed bit and somebody else's certificate are refused. The signing time is covered by the signature, but it is the signer's word.

### 2. How an ECDSA signature is made, step by step

The inputs are the statement and a private key (the RFC 6979 test key c9afa9d8 ... 120f6721). Step 1 hashes the statement: h = SHA-256(message). Step 2 derives the one-time number k from the key and h (RFC 6979). Step 3 takes r, the x coordinate of the curve point k times G.
Step 4 computes s = (h + r times key) / k, modulo n. The signature is r and s, 64 bytes. The check uses only the public key Q: the x of (h/s) G + (r/s) Q equals r. A changed balance gives another x, so it is refused. The steps give Bouncy Castle's exact signature, and RFC 6979's published r and s for "sample".

### 3. The nonce that must never repeat

The mistake: "pay 10 to Bob" and "pay 99 to Eve" are signed with the same k, so both signatures have the same r. The attack, without the key: k = (h1 - h2) / (s1 - s2) gives 12345678 90abcdef ..., and key = (s1 times k - h1) / r gives c9afa9d8 ... 120f6721, the bank's private key.
The fix: a deterministic k (RFC 6979) gives the same signature for the same statement, so it never repeats on different messages; a random k gives different signatures that both verify. Ed25519 (64 bytes) and Ed448 (114 bytes) match RFC 8032, sign the same bytes every time, and refuse a flipped bit.

### 4. A compact signed token: JWS

Before signing: the header {"alg":"EdDSA"} and the payload "Example of Ed25519 signing". After signing, each part is base64url: eyJhbGciOiJFZERTQSJ9, RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc, and an Ed25519 signature over "header.payload". The three parts joined by dots are exactly the token in RFC 8037, and it verifies.
The detached form has an empty middle part and verifies when the payload is supplied. A header that says alg none with no signature is refused, and so is the same signature on another payload. The verifier decides the algorithm, not the token.

### 5. Text armor, and the bill

Before armor, the signed CMS message is binary bytes. After armor, PEM writes it as Base64 text between -----BEGIN CMS----- and -----END CMS-----, no line over 64 characters, and it reads back to the same bytes.
The bill: a valid signature does not say who owns the key (that needs a chain of certificates), when it was signed (that needs a timestamp authority, RFC 3161), or where the private key lives (keystore-basics). OpenSSL checks the same files in the integration tests.

## The verdict

Use a standard format, deterministic signatures (Ed25519 or deterministic ECDSA), a verifier that fixes the expected algorithm, and a certificate chain that says who owns the key. Add a trusted timestamp when the time matters, and protect the private key.

## How to recognise this in code you did not write

- `openssl cms -sign` and `jarsigner` in build scripts.
- Signed PDFs and S/MIME mail.
- JWT libraries that ask you to pass the allowed algorithms.
- Reports of ECDSA keys recovered from bad random number generators.

## Where you have already met this

- Signed PDFs and S/MIME email, which are CMS SignedData.
- Java's `jarsigner`, and Android's original (v1) APK signing scheme.
- JWT and JWS tokens in web logins, and `alg: none` in vulnerability reports.
- `openssl dgst -sign` and `openssl cms -sign` in scripts.
