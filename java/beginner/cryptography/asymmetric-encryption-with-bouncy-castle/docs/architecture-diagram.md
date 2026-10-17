# Asymmetric Encryption with Bouncy Castle and OpenSSL — Architecture Diagram

The statement is encrypted with AES-256; the AES key is locked with the RSA key in Ana's certificate; both go in one standard envelope.

![A CMS envelope for Ana](images/architecture-diagram.png)


