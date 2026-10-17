# Asymmetric Encryption with Bouncy Castle and OpenSSL, Explained

## The idea in one sentence

Bouncy Castle adds to the JDK the standard formats that asymmetric encryption is used in: PEM text for keys and certificates, X.509 certificates, and the CMS envelope that holds data encrypted with AES and its key locked with RSA, so that independent programs such as OpenSSL can open what it writes.

## The 5 acts

### 1. Keys as PEM text

Ana's public key written as PEM begins -----BEGIN PUBLIC KEY-----, and reads back as the same key. Her private key as PEM begins -----BEGIN PRIVATE KEY-----, and anyone who can read the file has the key. Encrypted with a password it
begins -----BEGIN ENCRYPTED PRIVATE KEY-----: with the right password it reads back as the same key, and with a wrong password it is refused. The JDK has no PEM support, which is the first thing
Bouncy Castle adds. Text keys are easy to move, and easy to leak.

### 2. A certificate for the public key

A certificate binds a public key to a name, and a validity period, and is signed. Ana's is made with Bouncy Castle: the subject is CN=Ana,O=Example Bank customer, valid from 2026-01-01 to 2027-01-01, signed with SHA256WITHRSA. The issuer
is the same as the subject: the certificate is signed by its own key. Anyone can make one, so it proves nothing about who Ana is until someone trusted signs it, which is the subject of the next projects. CMS uses the certificate to name the recipient of an envelope.

### 3. A standard envelope: CMS EnvelopedData

CMS EnvelopedData is the standard hybrid envelope, the format behind S/MIME email. The bank seals the 10,000-byte statement for Ana's certificate: AES-256 encrypts the data, and RSA-OAEP locks the AES key, and the
certificate names the recipient. Ana opens it with her private key. Eve tries her own private key and is refused, because the envelope is not addressed to her certificate and her key cannot open the locked key.

### 4. A different program opens it

How can anyone tell the envelope is really in the standard format, and not just something only this library can read? Ask a different program. The bank writes the envelope, Ana's key file and her certificate to a temporary folder, and runs
openssl cms -decrypt on them. OpenSSL, written by other people, opens the statement, and the bytes match the original. A format that only one library understands is a trap. A standard is one that independent programs agree on.

### 5. The bill: what Bouncy Castle costs

Bouncy Castle has costs. The envelope in the demo locks its key with RSA-OAEP, OID 1.2.840.113549.1.1.7, because the code asked for it. The simple call that most examples show locks the key with RSA PKCS#1 v1.5 padding, OID
1.2.840.113549.1.1.1, which is older and weaker. The easy call picks the older padding. The library is about 7 MB, and registering it as a provider changes every library in the program. Its artifact names changed from jdk15on to jdk18on, so
upgrades need care. And the JDK alone could do the maths, but not the PEM, the certificate or the CMS format, which is what the library is for.

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
