# Asymmetric Encryption (RSA and Key Agreement) — Data Flow Diagram

Each side combines its own private key with the other's public key; the results match, and HKDF turns the secret into a key.

![How two sides get the same key without sending it](images/data-flow-diagram.png)


