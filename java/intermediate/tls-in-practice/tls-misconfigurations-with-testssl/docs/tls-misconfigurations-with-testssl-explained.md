# TLS Misconfigurations with testssl.sh, Explained

## The idea in one sentence

A scanner reports what a server does; a policy says what the shop requires; the gate is the policy applied to the report, with exceptions that someone signed.

## The 5 acts

### 1. A server that follows the guidance

testssl.sh reads the protocols, cipher suites, certificate and headers of one server in about fifteen seconds. The guided server offers TLS 1.2 and 1.3, a chain that passes, strong suites with forward secrecy and HSTS for 730 days. testssl itself calls one finding HIGH: no CRL or OCSP URI is provided. The shop's policy has eleven rules; this server breaks one of them, revocation, which has an exception because the lab authority publishes neither. The gate passes.

### 2. A configuration copied from the first search result

The copied configuration has the same certificate and the same protocols but no ssl_ciphers, no headers and no server_tokens off. It breaks four rules: obsolete cipher suites, HSTS, security headers and the version in the Server header. testssl rates them LOW or MEDIUM. Nothing here stops a browser from loading the page: all of it is invisible to the person using it. The guided configuration with the same certificate passes.

### 3. A server that still speaks what old clients speak

A server written with the JDK, which has to be told to do it, offers TLS 1.0, 1.1 and 1.2, and only RSA key exchange. Forward secrecy with strong suites is not offered (MEDIUM): a stolen key opens every recorded session. testssl rates TLS 1.0 and 1.1 as LOW, and the shop's policy refuses them: the rating is the tool's, the rule is yours. It breaks protocols, TLS 1.3, forward secrecy, obsolete suites, HSTS and headers.

### 4. Certificates that clients will refuse

An expired certificate: the chain is CRITICAL (failed, expired) and so is the expiry. A certificate for pay.shop.localhost on www.shop.localhost is HIGH: it does not match the name asked for. Each breaks its own rule of the policy. Both of these stop every browser, and nothing else about the server matters until they are fixed.

### 5. The bill

The same good server scanned by its address instead of its name fails the name rule: a false alarm, because the certificate is not for an address. An exception is a decision written down with its reason. A scan is a snapshot of one moment, of the handshake and of the page at /. The policy is a file under version control, and the scan is rerun after every change and on a schedule, because a certificate runs out without anyone touching it.

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
