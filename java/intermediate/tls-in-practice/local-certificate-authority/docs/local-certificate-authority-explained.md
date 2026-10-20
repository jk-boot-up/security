# A Local Certificate Authority, Explained

## The idea in one sentence

A certificate authority signs a key-to-name claim; clients check the chain, dates, name, usage and a current revocation list, and short lifetimes shrink the damage of a stolen key more reliably than revocation.

## The 5 acts

### 1. Issue a certificate and trust the root

The root is self-signed and valid for 3650 days. The CA signs a leaf certificate with serial 1000, valid for 90 days, for the names shop.example and *.shop.example. A client that was given the root certificate checks the chain,
the dates and the name, with a fresh revocation list, and accepts.

### 2. What a client refuses

www.shop.example matches the wildcard, a.b.shop.example does not (the wildcard covers one label), and evil.example is not in the certificate. 100 days later the certificate has expired. A certificate from another authority, one that claims our
authority's name but is signed by another key, and a self-signed server certificate are not trusted. A leaf certificate cannot sign another certificate, because its basic constraints say it is not a CA.

### 3. Revocation

The CA revokes serial 1000 on day 10 and publishes list number 2. A client with that list refuses the certificate. A client that has only the list from day 0 (now stale), no list at all, or the new list two days after its next update
cannot know the status, and refuses as well: hard fail. A client that treated a missing list as "fine" would keep trusting the stolen key. The cost is that a CA outage becomes a connection outage.

### 4. Renewal

Renewal is a new certificate for the same key and the same names with a new serial number and new dates, issued before the old one expires (here on day 80). On day 100 the old certificate is refused as expired and the renewed one is accepted.
Renewing with the same key is easy, but renewing with a new key limits the damage of an unnoticed theft.

### 5. Short-lived certificates instead of revocation

If the CA publishes a list every 24 hours, a stolen key can be used until clients fetch the new list: up to 24 hours after the theft is noticed. If the certificates themselves last 8 hours, the stolen key stops working after at most 8 hours
whatever anyone fetches. The 8-hour certificate is accepted now and refused as expired nine hours later. This is why modern private CAs favour short lives and automatic renewal.

## The verdict

Use a public CA with ACME for public names. For private names, run a CA with an offline root and an intermediate for issuing, short lifetimes with automatic renewal, name constraints, and clients that hard-fail on missing revocation data where they check it.

## How to recognise this in code you did not write

- `-----BEGIN CERTIFICATE-----` files and `keytool -importcert`.
- `X509v3 Subject Alternative Name` and `X509v3 Basic Constraints: CA:TRUE` in `openssl x509 -text`.
- ACME clients renewing every 60 days.
- A finding titled "Improper Certificate Validation" with CWE-295.

## Where you have already met this

- The padlock and "Certificate details" in a browser.
- `keytool -genkeypair`, `openssl req -x509` and `-----BEGIN CERTIFICATE-----`.
- "Your connection is not private" with NET::ERR_CERT_DATE_INVALID or a name mismatch.
- CRL and OCSP in the certificate's authority information.
