# Finding TLS Misconfigurations, Explained

## The idea in one sentence

TLS security is mostly configuration, so check protocols, suites, certificates and HTTP settings with named rules on a schedule, start from a published policy, and fix the certificate and header problems that no policy can.

## The 4 acts

### 1. A server that follows the guidance

shop.example offers TLS 1.2 and 1.3, only ECDHE and TLS 1.3 AEAD suites, chooses the suite itself, has no compression, supports secure renegotiation, a valid EC certificate with a complete chain, a name that matches, HSTS for two years including subdomains,
a redirect from HTTP, and a Secure cookie. The report is grade A, score 100, no findings.

### 2. A legacy server

legacy.shop.example still offers TLS 1.0 and 1.1, RC4 and 3DES suites, a static RSA suite with no forward secrecy and CBC suites, lets the client choose the suite, enables compression, lacks secure renegotiation, uses a 1024-bit RSA key with a SHA-1 signature, has no
redirect from HTTP and an HSTS of one day. Thirteen findings, worst first: six rated high plus the rest, and the grade is F.

### 3. A certificate that clients will refuse, and a forgotten HTTP setup

pay.shop.example expired 10 days ago, is issued for www.other.example, and does not send its intermediate: two critical findings and a high one, grade F no matter how good the protocols are. blog.shop.example has a valid but incomplete chain, expires in 10 days,
has no HSTS, no redirect and a cookie without Secure: five findings and a D. A checker finds in seconds what users find as an error page.

### 4. Moving the legacy server to a policy

A policy replaces the protocol list and the suite list, makes the server choose the suite, and turns off compression and insecure renegotiation. Applied to the legacy server it removes nine of the thirteen findings. Four remain, because they are not protocol settings:
the weak key, the SHA-1 signature, the missing redirect and the short HSTS. The policy also generates an nginx fragment with the same protocols, suites, server preference and a two-year HSTS header.

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
