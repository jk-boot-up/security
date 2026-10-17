# Asymmetric Encryption (RSA and Key Agreement), Explained

## The idea in one sentence

Asymmetric encryption uses a public key that anyone may have and a private key that only its owner keeps, so two parties who have never met can lock a key to each other with RSA and OAEP, carry large data with hybrid encryption, or agree a key with X25519 and HKDF without sending one, but nothing in the maths tells them whose public key it is.

## The 5 acts

### 1. The problem: sharing a key

The bank wants to send Ana an encrypted statement, so first it must give her the key. It sends the key over the open network. Eve sees every message on the wire, including the key,
and can open anything encrypted with it from then on. A secret key cannot be sent in the clear, and the bank and Ana have never met, so there is no safe way to hand it over.

### 2. A key pair: lock with the public key, open with the private key

Ana makes a key pair. Her public key, 294 bytes, is published, and her private key stays with her. The bank locks the 32-byte key with the public key using RSA with OAEP padding, and 256 bytes go over the wire. Ana opens it with
her private key. Eve has the public key and the locked message, and tries her own private key: it is refused. Locking the same key again gives different bytes, because OAEP adds randomness. And only 190
bytes fit in a 2048-bit RSA lock, one byte more is refused. RSA is for carrying a key, not data.

### 3. Hybrid encryption: RSA for the key, AES for the data

A statement is far longer than 190 bytes, so the bank does the heavy work with AES. It makes a fresh 256-bit AES key for this one message, encrypts the 10,000-byte statement with AES-GCM, and locks only that small key with
Ana's RSA public key. The envelope holds 256 bytes of locked key, 12 of nonce and 10,016 of cipher text and tag: 10,284 bytes in all. Ana opens it. Eve's private key is refused, and a flipped bit is refused, because the AES-GCM tag covers
the data. Every message gets its own data key, so one message's key never opens another.

### 4. Key agreement: no key is sent at all

Key agreement sends no key. The bank and Ana each make an X25519 key pair and swap only the public keys, 44 bytes each. Each combines its own private key with the other's public key, and the two get the same secret, which HKDF turns into
the same 32-byte key. Eve saw both public keys, and makes a key pair of her own. Her key comes out different, because she lacks either private key. And a new session, with a new key pair on the bank's side, gives a completely different key, so an old
session's key is not part of the new one. That is called forward secrecy.

### 5. The bill: whose key is it?

Nothing in RSA or X25519 says whose public key it is. Mallory sits on the wire and swaps Ana's public key for her own. The bank locks a message to the wrong key. Mallory reads it, PIN is 4821, locks it again for Ana, and sends it on. Ana opens what she receives,
and nobody notices. This is a man-in-the-middle attack, and the maths cannot stop it. Signatures and certificates, in the next projects, are the answer: they let a key arrive with proof of whose it is. Other costs: an RSA-2048 public key is 294 bytes against
44 for X25519, and a stolen private key opens every message ever locked to its public key.

## The verdict

Use hybrid encryption, with RSA-OAEP or an elliptic-curve key agreement, never raw RSA and never data locked by RSA directly. Use fresh keys for each session, so a later theft does not reveal old messages. Keep private
keys private, and always check, with a signature or a certificate, that a public key belongs to the party you think it does.

## How to recognise this in code you did not write

- `KeyPairGenerator.getInstance("RSA")` and `Cipher.getInstance("RSA/ECB/OAEPPadding")`.
- `KeyAgreement.getInstance("X25519")` followed by a key derivation.
- An envelope with a locked key, a nonce and a cipher text.
- A `.pub` file next to a private key file.

## Where you have already met this

- Every HTTPS connection, which begins with a key agreement like the fourth act.
- PGP and S/MIME email encryption, which use hybrid encryption with the recipient's public key.
- SSH keys: `id_rsa.pub` is a public key, and `id_rsa` is the private one.
- The padlock in the browser, which is the result of keys being checked by a certificate.
