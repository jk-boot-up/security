# Certificates and the Chain of Trust, Explained

## The idea in one sentence

A certificate is an issuer's signature over a name and a public key, a chain of them leads to a root the client trusts without proof, and a client must also check dates, permissions, revocation and the host name.

## The 5 acts

### 1. A chain of three certificates

Before signing, the shop asks for a name, CN=shop.example for the site shop.example, its own public key, an EC key of 256 bits, and the dates 2025-06-01 to 2026-06-01. After signing, the certificate with serial 3 holds the same three, plus the issuer, CN=Example Issuing CA, and the issuer's SHA256withECDSA signature over all of it, made with the issuer's private key. Anyone can check that signature with the issuer's public key.
The chain from the root down: Example Root CA signs itself; Example Issuing CA is signed by the root and is a CA, so it may issue certificates; shop.example is signed by the intermediate and is not a CA.

### 2. The browser's check, step by step

Before the check, the browser has what the server sent, its own certificate shop.example and the intermediate Example Issuing CA, the root it already trusts, Example Root CA, and the site it is visiting, shop.example, on 2026-01-01.
Step 1 checks shop.example against Example Issuing CA: it names it as the issuer, its signature checks with that public key, and 2026-01-01 is inside 2025-06-01 to 2026-06-01. Step 2 checks Example Issuing CA against Example Root CA the same way, and also that it may issue certificates. Step 3: the root is in the trust store, and shop.example is in the certificate.
After the check, the class ChainSteps, which makes these steps by hand, accepts the chain, and so does the JDK's PKIX validator. A wildcard *.example matches shop.example but not a.b.example: it stands for exactly one label.

### 3. Three reasons to refuse

Before the check, the browser is shown three chains: a certificate for shop.example valid only from 2024-01-01 to 2024-12-31; a certificate for shop.example from a rogue root that is also named Example Root CA; and the good chain, shown to a visitor who meant evil.example.
After the check: the first fails at the dates, and the JDK says EXPIRED. The second fails at the signature, and the JDK says INVALID_SIGNATURE: the issuer's name matched, but the key did not. The third chain is accepted, but the name evil.example does not match. Names prove nothing, only keys do, and the chain and the name are two separate checks.

### 4. Changed certificates, and certificates that overstep

Before the change, the signed name is CN=shop.example. After changing one letter, CN=shop.examplE, the certificate still parses, but it fails at the signature, and the JDK says INVALID_SIGNATURE: the signature covers every byte.
Before the next check, the shop's server key signs a certificate for other.example, but the shop's certificate is not a CA. After the check, it fails at the CA flag, and the JDK says INVALID_KEY_USAGE. The intermediate itself allows 0 more CAs below it: its path length.

### 5. Revocation, and the bill

Before revocation, a thief has the intermediate's private key and issues their own certificate for shop.example, serial 77. The forged chain is accepted: a stolen CA key breaks everything below it.
After revocation: with serial 3, the shop's, on the client's list, the shop's chain is refused, REVOKED. With serial 2, the intermediate's, on the list, the shop's chain and the forged chain are both refused.
The bill: a list works only if the client has the latest one, so real clients use CRLs, OCSP, short lifetimes, or all three; a trusted root may vouch for any name, so keep the store small; and keep CA keys offline or in hardware.

## The verdict

Rely on a real certificate authority and the platform's validator. Check the host name every time. Keep the trusted root list small, issue short-lived certificates, keep authority keys offline or in hardware, and have a way to revoke.

## How to recognise this in code you did not write

- The browser's certificate viewer, showing the chain.
- `keytool -list` and the `cacerts` file.
- ACME clients renewing certificates automatically.
- Certificate transparency logs that record every certificate issued.

## Where you have already met this

- The padlock in the browser, and the "your connection is not private" warning.
- `cacerts`, the Java file listing the roots the JDK trusts, and the roots in your operating system.
- Let's Encrypt and other certificate authorities, and their 90-day certificates.
- Certificate-expiry outages in the news.
