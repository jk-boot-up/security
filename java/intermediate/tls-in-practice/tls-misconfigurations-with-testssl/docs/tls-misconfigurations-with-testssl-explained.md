# TLS Misconfigurations with testssl.sh, Explained

## The idea in one sentence

A scanner reports what a server does; a policy says what the shop requires; the gate is the policy applied to the report, with exceptions that someone signed.

## The 5 acts

### 1. A server that follows the guidance

The input is nginx with the guided lines: ssl_protocols TLSv1.2 TLSv1.3, six cipher suites that all use ECDHE, HSTS and four security headers, server_tokens off, and a certificate for www.shop.localhost from the lab's root. testssl.sh runs with -p (protocols), -s (cipher lists), -S (certificate) and -h (headers), and writes one JSON row per check: an id, a severity and a finding.
The result: every row is OK or INFO except cert_revocation, HIGH, "Neither CRL nor OCSP URI provided". The shop's eleven rules each read one id; ten hold and revocation is broken. Revocation has an exception with its reason (the lab authority publishes no CRL or OCSP responder), so the gate passes.

### 2. A configuration copied from the first search result

The input is the copied configuration: the same certificate and the same ssl_protocols, but no ssl_ciphers (nginx's default list), no add_header lines and server_tokens on, the default.
The report changes in four rows: obsolete cipher suites LOW (offered), HSTS LOW (not offered), security headers MEDIUM, and a Server banner with nginx's version. The HSTS time of the guided server, worked out: max-age 63072000 seconds / 86400 = 730 days, above testssl's minimum of 15552000 seconds = 180 days, so OK; the copied server sends no header, so LOW.
The result: testssl's HIGH or CRITICAL list is the same as for the guided server, but the shop's gate fails on obsolete-suites, hsts, headers and banner. A browser loads the page just the same.

### 3. A server that still speaks what old clients speak

The input is a server written with the JDK and told to allow what it refuses by default: TLSv1, TLSv1.1 and TLSv1.2, and only TLS_RSA_WITH_* suites, so the key exchange is RSA, with an RSA 2048-bit certificate.
The report: TLS1 and TLS1_1 LOW (offered, deprecated), TLS 1.3 not offered, strong suites without forward secrecy offered, strong suites with forward secrecy not offered (MEDIUM), obsolete suites LOW, no HSTS and no security headers.
The result: the same finding has two readers. testssl calls TLS 1.0 LOW; the shop's protocols rule is broken. A gate that fails on testssl's HIGH or CRITICAL sees [cert_revocation] for the guided, the copied and the legacy server alike. The shop's gate passes the guided one, fails the copied one on four rules and the legacy one on six: protocols, tls13, forward-secrecy, obsolete-suites, hsts and headers.

### 4. Certificates that clients will refuse

The input is two certificates from the lab's root on otherwise guided nginx servers: one valid from 72 hours ago to 24 hours ago, one for pay.shop.localhost.
The expiry, worked out: not after minus not before is 2161 hours = 90 days for the good certificate (89 days left, at least 60, so OK) and 48 hours for the expired one, which ended before the scan, so the expiry is CRITICAL and the chain fails with it.
The name, worked out: the name asked for is www.shop.localhost and the certificate lists only pay.shop.localhost, so it is not listed and cert_trust is HIGH (a *.shop.localhost wildcard would cover it, one label deep).
The result: the expired certificate fails the chain and expiry rules, the other one fails the name rule, and every browser refuses both.

### 5. The bill

The input is the same guided server, scanned with 127.0.0.1:<port> instead of --ip 127.0.0.1 www.shop.localhost:<port>.
The report: cert_trust HIGH, "certificate does not match supplied URI". Worked out: the name asked for is 127.0.0.1, the certificate lists www.shop.localhost, so it is not listed.
The result: by name the gate passes, by address it fails on the name rule; a false alarm, because the server did not change. A scan does not see the application, the other paths or tomorrow's change, so it is run by name, after every change and on a schedule.

## The verdict

Scan by name, keep the policy in version control, make every exception carry a reason, rerun after every change and on a schedule, and trust a finding only after you know what the tool did to find it.

## How to recognise this in code you did not write

- `testssl.sh --jsonfile` in a pipeline.
- A report with LOW, MEDIUM, HIGH and CRITICAL findings.
- An ignore list with no reasons in it.
- A certificate that expired unnoticed.

## Where you have already met this

- A report from an online SSL test tool, with a letter grade.
- `ssl_protocols` and `ssl_ciphers` lines copied from a blog post.
- A scanner step in a pipeline that fails on severity HIGH.
- A certificate that expired on a Sunday.
