# TLS Attacks and Downgrades, Explained

## The idea in one sentence

Offering an old thing is the weakness, even if no client uses it today, so count what a server accepts against what it should.

## The 5 acts

### 1. The weak server

The weak server accepts TLS 1.0 and 1.1, static RSA, CBC with SHA-1, a 1024-bit finite-field DH group and a NULL cipher. openssl s_client probes show what each of the three servers accepts: 4 protocol versions and 8 cipher suites, as yes and no.

### 2. Probes and testssl.sh

Java reads the testssl.sh JSON and counts it against the planted list. TLS 1.0, TLS 1.1, the NULL cipher and the CBC suites have a finding of their own. Static RSA and the 1024-bit group have none: they show only in the key exchange column of the cipher table. The strong server raises nothing.

### 3. The downgrade dance

A middlebox breaks every handshake above TLS 1.0. The retrying client needs 4 attempts and ends on TLS 1.0 with a static RSA CBC suite. With TLS_FALLBACK_SCSV the server answers inappropriate fallback. A client with a minimum of TLS 1.2 stops after 2 attempts. The JDK client never retries. A TLS 1.3 server that is pushed down marks its ServerHello with DOWNGRD.

### 4. The leaked key

The demo records three sessions through a tap. Java derives the TLS 1.2 keys from the RSA-decrypted pre-master secret and decrypts the AES-GCM record: the request is read for the static RSA session. For DHE and ECDHE the secret was agreed from keys that were thrown away, and the leaked key opens nothing. A key that is not the server's opens none.

### 5. What TLS 1.3 removed

The TLS 1.3 only server answers protocol version to TLS 1.0, 1.1 and 1.2, and refuses a static RSA and a CBC suite. None of the 3 TLS 1.3 suites has a fixed key exchange or lacks AEAD. A TLS 1.3 hello offers only the null compression method, the client library refuses to renegotiate, and the 1024-bit group in the server's file is not used. RC4 and zlib are not in this OpenSSL build.

## The verdict

Allow TLS 1.2 and 1.3 only, with ephemeral key exchange and AEAD, let the clients send the fallback signal or never retry, scan with testssl.sh on every release, and read its cipher table and not only its grade.

## How to recognise this in code you did not write

- A grade of A with a static RSA suite in the table.
- A client that retries with a lower version.
- A 1024-bit DH file in a web server configuration.
- A partner endpoint that still lists TLS 1.0.

## Where you have already met this

- A load balancer that still lists TLS 1.0 for one partner.
- A scan report with a green grade and a static RSA suite in the middle of it.
- A client library that retries with a lower version on any error.
- A configuration with a 1024-bit DH file nobody has opened since 2015.
