# Asymmetric Encryption with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

Bouncy Castle adds to the JDK the standard formats that asymmetric encryption is used in: PEM text for keys and certificates, X.509 certificates, and the CMS envelope that holds data encrypted with AES and its key locked with RSA, so that independent programs such as OpenSSL can open what it writes.

## The 6 acts

### 1. The public key as PEM text

A key is bytes, not text: Ana's public key is 294 bytes in the DER encoding, beginning 30820122 300d0609. PEM writes those bytes as Base64 letters, 64 to a line, between
-----BEGIN PUBLIC KEY----- and -----END PUBLIC KEY-----: 392 letters in 7 lines. Base64 takes 3 bytes, 24 bits, at a time, and cuts them into four groups of 6 bits. Each group is a
number from 0 to 63, which picks one letter of the alphabet A to Z, a to z, 0 to 9, + and /. The first bytes 30 82 01 are the bits 00110000 10000010 00000001, the groups
001100 001000 001000 000001, the numbers 12 8 8 1, and the letters M I I B. Done by hand for all 294 bytes, it gives exactly the library's PEM text. PEM hides nothing: it only makes bytes safe to paste.

### 2. The private key file, locked with a password

Ana's private key as PEM begins -----BEGIN PRIVATE KEY----- and reads back without any password: whoever can read the file has the key. Locked with a password, it begins
-----BEGIN ENCRYPTED PRIVATE KEY-----: the key inside is encrypted with AES-256, under a key made from the password and a random salt. With the right password it reads back as the
same key, and with a wrong password it is refused. The JDK has no public way to write this file; Bouncy Castle does.

### 3. A certificate for the public key

A certificate binds a public key to a name and a validity period, and is signed. The inputs: the name CN=Ana,O=Example Bank customer, Ana's 294-byte public key, and the dates
2026-01-01 to 2027-01-01. Bouncy Castle signs them with SHA256WITHRSA, using Ana's own private key, and gives a 749-byte certificate with serial number 1. Its issuer, the one who
signed, is the same as its subject: Ana vouches for herself. Anyone can make one, so it proves nothing about who Ana is until someone trusted signs it, which is the subject of the
next projects. The CMS envelope uses the certificate to name its recipient. The JDK can read certificates but has no public way to make one.

### 4. A standard envelope: CMS EnvelopedData

CMS EnvelopedData is the standard hybrid envelope, the format behind S/MIME email. Before sealing: a 10,000-byte statement, 625 lines of RENT -1200, and Ana's certificate. Bouncy Castle
makes a new AES-256 key, encrypts the statement with it in CBC mode, and locks the AES key with Ana's RSA public key, using OAEP. After sealing, one 10,520-byte envelope holds four
parts: who it is for (the certificate's issuer and serial number 1), the 256-byte locked key, a 16-byte IV (the start value for CBC), and 10,016 bytes of cipher text. Ana opens it with her
private key. Eve tries her own private key and is refused. AES carries the big data, RSA only the small key: that is hybrid encryption.

### 5. Opening it by hand, then with openssl

The library hides how an envelope opens, so a small class, EnvelopeSteps, redoes it with the JDK's own ciphers. Step 1: RSA-OAEP with Ana's private key turns the 256-byte locked key into a
32-byte, 256-bit AES key. Step 2: AES-256-CBC, with that key and the IV, turns the 10,016 bytes of cipher text into 10,016 bytes. Their last block is sixteen bytes that are all 10 in hex:
padding. Step 3: the last byte, 0x10 = 16, says how many bytes to remove, leaving the 10,000-byte statement, exactly what Bouncy Castle gave. Then the openssl command-line tool, written by
other people, is given the envelope, Ana's key file and her certificate, and opens the statement too. A standard is a format that independent programs agree on.

### 6. The bill: what Bouncy Castle costs

Bouncy Castle has costs. The envelope in the demo locks its key with RSA-OAEP, OID 1.2.840.113549.1.1.7, because the code asked for it by name. The simple call that most examples show locks
the key with RSA and the older PKCS#1 v1.5 padding, OID 1.2.840.113549.1.1.1. The library is about 7 MB, and registering it as a provider is global: every library in the program can be handed
its algorithms. Its artifact names changed from jdk15on to jdk18on, so upgrades need care. And the JDK cannot make a certificate and has no CMS, which is what the library is for.

## The verdict

Use the JDK for the maths, and a library such as Bouncy Castle only for the formats the JDK does not know. Ask for OAEP by name, protect private-key files with a password, keep the library
pinned and updated, and always test interoperability with an independent program, because a private format is a liability.

## How to recognise this in code you did not write

- `JcaPEMWriter`, `PEMParser` and `JcaX509v3CertificateBuilder` in Java code.
- `Security.addProvider(new BouncyCastleProvider())` near the start of a program.
- `openssl cms` and `openssl x509 -text` in operations documentation.
- Files whose first line is `-----BEGIN ... -----`.

## Where you have already met this

- `-----BEGIN CERTIFICATE-----` and `-----BEGIN PRIVATE KEY-----` in files on a web server.
- `openssl cms -encrypt` and `-decrypt`, and S/MIME encrypted email.
- `id_rsa` and `id_rsa.pub` in an `.ssh` folder, which are PEM-style files.
- Libraries named `bcprov` and `bcpkix` in the dependency lists of Java applications.
