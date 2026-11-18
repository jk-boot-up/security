# Finding TLS Misconfigurations, Explained

## The idea in one sentence

TLS security is mostly configuration, so check protocols, suites, certificates and HTTP settings with named rules on a schedule, start from a published policy, and fix the certificate and header problems that no policy can.

## The 4 acts

### 1. A server that follows the guidance

The input is what a scan learned about shop.example: protocols TLSv1.2 and TLSv1.3, nine suites that are all forward secret, an EC 256-bit certificate signed with SHA256withECDSA for shop.example and *.shop.example, valid from 2026-01-30 to 2026-04-30 (today is 2026-03-01), a complete chain, HSTS with max-age 63072000 (730 days) and subdomains, a redirect from HTTP and a Secure cookie.
The result: no findings, so 100 points stay 100, and the score gives grade A.
A suite name lists its parts: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 is key exchange ECDHE, authentication RSA, cipher AES_128, mode GCM, hash SHA256. TLS_RSA_WITH_RC4_128_SHA has RSA key exchange (no forward secrecy) and the broken RC4 stream cipher.

### 2. A legacy server

The input is what a scan learned about legacy.shop.example: TLSv1, TLSv1.1 and TLSv1.2; five suites read by name: RC4 and 3DES are broken, TLS_RSA_WITH_AES_128_CBC_SHA has no forward secrecy, an ECDHE CBC suite and one good GCM suite; an RSA 1024-bit key signed with SHA1withRSA; HSTS of one day; no redirect; the client picks the suite; compression on; insecure renegotiation.
The result is 13 findings, worst first: seven HIGH at 20 points, two MEDIUM at 7, four LOW at 2. The points: 100 - 7 x 20 - 2 x 7 - 4 x 2 = -62; the score never goes below 0, and a score under 40 is grade F.

### 3. A certificate that clients will refuse, and a forgotten HTTP setup

pay.shop.example has the same protocols, suites and features as shop.example. Its certificate names www.other.example, is valid from 2024-10-17 to 2026-02-19, and the server does not send the intermediate certificate. The checks: 2026-02-19 to 2026-03-01 is 10 days after the end, so it expired; pay.shop.example is not in [www.other.example]; a wildcard *.shop.example would cover pay.shop.example but not a.pay.shop.example, because it covers one label only.
The report: cert-expired and name-mismatch are CRITICAL, incomplete-chain is HIGH. 100 - 2 x 60 - 1 x 20 = -40, so the score is 0, and any CRITICAL finding gives F however good the protocols are.
blog.shop.example is valid until 2026-03-11, 10 days from today, sends no intermediate, has no HSTS header, no redirect and a cookie without Secure: one HIGH and four MEDIUM findings, 100 - 1 x 20 - 4 x 7 = 52, grade D.

### 4. Moving the legacy server to a policy

Before the policy, legacy.shop.example has grade F, score 0 and 13 findings. The intermediate policy sets the protocols TLSv1.2 and TLSv1.3, nine suites that are all forward secret and none broken, and the features: server picks, compression off, secure renegotiation.
After the policy the server has grade D, score 51 and 4 findings. Fixed by the policy: broken-cipher, insecure-renegotiation, old-protocol-tlsv1, old-protocol-tlsv1_1, tls-compression, no-forward-secrecy, cbc-suites, client-chooses-cipher and no-tls13. Left to fix by hand: weak-key, weak-signature, no-https-redirect and short-hsts, because they are not protocol settings.
The policy also writes the nginx settings: ssl_protocols, the six ECDHE suites in OpenSSL names, server preference, no session tickets and a two-year Strict-Transport-Security header.

## The verdict

Offer TLS 1.2 and 1.3 only, with forward-secret AEAD suites, from a published profile. Send the complete chain, watch expiry with an alert well before the date, set HSTS with a growing max-age, redirect HTTP, mark cookies Secure, and run a checker in CI and on a schedule.

## How to recognise this in code you did not write

- `ssl_protocols`, `ssl_ciphers` and `ssl_prefer_server_ciphers` in nginx.
- `Strict-Transport-Security: max-age=...; includeSubDomains`.
- Reports from SSL Labs, testssl.sh and Qualys.
- A finding titled "Weak SSL/TLS Configuration" with CWE-326 or CWE-327.

## Where you have already met this

- A grade A+ to F from SSL Labs.
- `ssl_protocols`, `ssl_ciphers` and `Strict-Transport-Security` in nginx and Apache configurations.
- Browser errors: NET::ERR_CERT_DATE_INVALID, ERR_CERT_COMMON_NAME_INVALID and "incomplete chain".
- `testssl.sh` and `nmap --script ssl-enum-ciphers`.
