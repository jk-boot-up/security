# Post-Quantum Cryptography, Explained

## The idea in one sentence

Recorded traffic can be opened later, so move key exchange first, move it as a hybrid, and write down where the old algorithms are.

## The 5 acts

### 1. What the new algorithms cost

The sizes come from real keys, ciphertexts and signatures made in the demo. ML-KEM-768 needs a 1206-byte public key and a 1088-byte ciphertext, 27.4 and 24.7 times X25519. ML-DSA-65 has a 1974-byte public key and a 3309-byte signature. Two certificates plus the handshake signature grow from 374 bytes with ECDSA P-256 to 13875 bytes.

### 2. ML-KEM and implicit rejection

KeyPairGenerator ML-KEM-768 and the KEM class give a 1088-byte ciphertext and a 32-byte shared secret. With one bit flipped, decapsulation raises no error and returns a key of 32 bytes that differs from the sender's; the first use, an AES-GCM tag check, fails. Only a ciphertext of the wrong length raises DecapsulateException.

### 3. ML-DSA and a hybrid exchange

ML-DSA-65 signs a statement and verifies it; one changed bit, another bank's key and a signature from ML-DSA-44 all fail. The hybrid exchange joins the X25519 and ML-KEM secrets with HKDF-SHA256 and the whole transcript; an attacker given the X25519 secret, or given the ML-KEM secret, does not rebuild the key. A classical-only exchange would be rebuilt.

### 4. Harvest now, decrypt later

Secrecy years plus migration years against years to the threat. A card authorisation message needs 3 years and a loan contract needs 34. If the threat is 8 years away, 3 of 5 data classes are already exposed to recording today; at 15 and at 25 years, 1 of 5. The inputs are an illustration, not a prediction.

### 5. Inventory and two implementations

Scanning the main source of the beginner and intermediate tracks finds 10 projects naming RSA, 12 ECDSA, 5 ECDH, 2 X25519 and 7 Ed25519, and none naming ML-KEM or ML-DSA. Bouncy Castle 1.86 and the JDK exchange public keys, ciphertexts and signatures in both directions and agree; the JDK does not read a Bouncy Castle private key encoding.

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
