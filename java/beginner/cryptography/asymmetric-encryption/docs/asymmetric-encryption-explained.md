# Asymmetric Encryption (RSA and Key Agreement), Explained

## The idea in one sentence

Asymmetric encryption uses a public key that anyone may have and a private key that only its owner keeps, so two parties who have never met can lock a key to each other with RSA and OAEP, carry large data with hybrid encryption, or agree a key with X25519 and HKDF without sending one, but nothing in the maths tells them whose public key it is.

## The 5 acts

### 1. The problem: sharing a key

The bank wants to send Ana an encrypted statement, so first it must give her the key. It sends the 32-byte key, statement-key-32-bytes-long-demo, over the open network. Eve sees every message on the wire,
and her copy is exactly the key, so she can open anything encrypted with it from then on. The bank and Ana have never met, so there is no safe way to hand the key over.

### 2. A key pair: the public key locks, the private key opens

By hand, with tiny numbers: Ana picks two secret primes, 89 and 97. Her public key is n = 89 x 97 = 8633 and e = 17. Her private key is d = 497, chosen so that 17 x 497 = 8449, which is one more than 88 x 96.
The bank locks the PIN 4821 as 4821^17 mod 8633 = 4485, and Ana opens it as 4485^497 mod 8633 = 4821. Locking again gives 4485 again: textbook RSA has no randomness.
Real RSA does the same with 2048-bit numbers and OAEP padding. Ana's public key is 294 bytes. The bank locks a 32-byte key into 256 bytes. Ana's private key opens it, Eve's is refused, locking again gives different bytes
because OAEP adds randomness, and at most 190 bytes fit.

### 3. Hybrid encryption: RSA for the key, AES for the data

A statement is far longer than 190 bytes, so the bank does the heavy work with AES. It makes a fresh 32-byte AES key for this one message, encrypts the 10,000-byte statement with AES-GCM, and locks only that small key with
Ana's RSA public key. The envelope holds 256 bytes of locked key, 12 of nonce and 10,016 of cipher text and tag: 10,284 bytes in all. Ana opens it. Eve's private key is refused, and a flipped bit is refused, because the AES-GCM tag covers
the data. Every message gets its own data key, so one message's key never opens another.

### 4. Key agreement: no key is sent at all

Key agreement sends no key. By hand, with tiny numbers: everyone knows g = 5 and p = 23. The bank picks the secret 6 and sends 5^6 mod 23 = 8. Ana picks 15 and sends 5^15 mod 23 = 19. The bank works out 19^6 mod 23 = 2, and Ana
works out 8^15 mod 23 = 2: the same number, which neither sent. Eve saw 8 and 19, but without 6 or 15 she cannot make 2.
X25519 does the same on an elliptic curve. With the test keys of RFC 7748, the bank and Ana swap only their public keys, and both get the secret 4a5d9d5b a4ce2de1 .... HKDF turns it into the 32-byte key 34501e3e d961431b ....
On the wire are only two public keys, 44 bytes each. Eve, with a key pair of her own, gets a different key. A new session, with a new bank key, gives a different key, so an old session's key is not part of the new one: forward secrecy.

### 5. The bill: whose key is it?

Nothing in RSA or X25519 says whose public key it is. Mallory sits on the wire and swaps Ana's public key for her own, so the bank locks the message PIN is 4821 to Mallory's key. Mallory reads it, locks it again with Ana's real public key, and sends it on.
Ana opens PIN is 4821, and nobody notices. This is a man-in-the-middle attack, and the maths cannot stop it. Signatures and certificates, in the next projects, are the answer. Other costs: an RSA-2048 public key is 294 bytes against
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

- A modern HTTPS connection, which begins with a key agreement like the fourth act.
- PGP and S/MIME email encryption, which use hybrid encryption with the recipient's public key.
- SSH keys: `id_rsa.pub` is a public key, and `id_rsa` is the private one.
- The padlock in the browser, which is the result of keys being checked by a certificate.
