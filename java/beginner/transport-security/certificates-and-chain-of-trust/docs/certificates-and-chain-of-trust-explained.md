# Certificates and the Chain of Trust, Explained

## The idea in one sentence

A certificate is an issuer's signature over a name and a public key, a chain of them leads to a root the client trusts without proof, and a client must also check dates, permissions, revocation and the host name.

## The 5 acts

### 1. A chain of three certificates

The root, Example Root CA, is signed by itself. The intermediate, Example Issuing CA, is signed by the root and may issue certificates. The server certificate, for shop.example, is signed by the intermediate and may not issue certificates.
Each certificate holds its owner's public key and the issuer's signature over it, both as plain data. The signature algorithm is SHA256 with ECDSA, and anyone can check it with the issuer's public key.

### 2. The browser's check

The server sends its own certificate and the intermediate. The browser already trusts the root, so the chain is accepted. The certificate is for shop.example and not for evil.example. A wildcard such as star dot example stands for exactly one label.

### 3. Three reasons to refuse

A certificate that ended on 2024-12-31, checked on 2026-01-01, is refused with the reason expired. A certificate for shop.example from an authority the browser does not trust is refused, even though the rogue root has the same name as the real one:
names prove nothing, only keys do. The same valid chain shown to a visitor who meant evil.example is accepted as a chain, and the name does not match. A client must check the chain and the name, and they are separate checks.

### 4. Changed certificates, and certificates that overstep

One letter of the name is changed. The certificate still parses, and it is refused because the signature no longer matches. Then the shop's server key signs a certificate for other.example. It is refused: a server certificate says it is not a certificate authority,
so what it signs is not trusted. The intermediate has a path length of zero, so it may not have further authorities below it.

### 5. Revocation, and the bill

Someone steals the intermediate's private key and issues their own certificate for shop.example. The forged chain is accepted: a stolen authority key breaks everything below it. Putting the shop's own certificate on the revocation list gets it refused with the reason revoked.
Putting the intermediate on the list refuses both the shop's chain and the forged one. A list only works if the client gets the latest one, so real clients use CRLs, OCSP, short lifetimes, or all three.
Trusting a root is a decision, because it may vouch for any name: keep the store small and keep authority keys offline or in hardware.

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
