# TLS Attacks and Downgrades, Explained

## The idea in one sentence

Offering an old thing is the weakness, even if no client uses it today, so count what a server accepts against what it should.

## The 5 acts

### 1. The lab: a weak server and two controls

The input is the lab: three openssl s_server processes on 127.0.0.1, made with OpenSSL 3.6, and testssl.sh v3.2.4 pinned to commit 97763a4. Six weaknesses are planted in the weak server: TLS 1.0, TLS 1.1, static RSA key exchange, CBC suites with SHA-1, a 1024-bit DH group and a NULL cipher.
The result is a table of 4 protocol versions and 8 cipher suites against the 3 servers, one s_client connection per cell. The weak server says yes to every old row; the strong server only to TLS 1.2, TLS 1.3 and the two ECDHE AEAD suites; the TLS 1.3 only server only to TLS 1.3.
The DH group size is read from s_client's line "Peer Temp Key: DH, 1024 bits"; under 2048 bits is a weakness.

### 2. Our probes and testssl.sh, scored against the truth

The input is the list of 6 planted weaknesses, the truth that each tool is scored against.
Our probes find each with one connection: -tls1 and -tls1_1 connect, AES128-GCM-SHA256, AES128-SHA and NULL-SHA connect, and the server sends a DH key of 1024 bits, under 2048. Found 6 of 6 on the weak server, 0 on the strong one.
testssl.sh's JSON report names TLS 1.0 and TLS 1.1 (findings TLS1 and TLS1_1, LOW), CBC with SHA-1 (LUCKY13, LOW) and the NULL cipher (its table row, CRITICAL). Static RSA (RSA in the Kx column, 4 rows) and the DH group (DH 1024) have no finding of their own. Found 6 of 6, 4 by a finding and 2 only in the table; the strong server has no finding of LOW or worse.

### 3. The downgrade dance

First, the server's own marker: asked for TLS 1.2, a server that can speak TLS 1.3 ends its ServerHello random with the bytes 44 4F 57 4E 47 52 44 01, "DOWNGRD" and 01, which means a downgrade to TLS 1.2. Asked for TLS 1.3, there is no marker.
The input of the dance is a network that resets every ClientHello offering more than TLS 1.0. A client that falls back with no signal tries up to TLS 1.3, TLS 1.2, TLS 1.1 and TLS 1.0, and connects on the fourth attempt with AES128-SHA.
The same client sending TLS_FALLBACK_SCSV on every retry is refused on the fourth attempt: the server can do better than TLS 1.0, so it answers inappropriate fallback. A client with a minimum of TLS 1.2 stops after 2 attempts and never offers TLS 1.0. The JDK client, with TLSv1.3 and TLSv1.2 enabled, fails and does not retry.

### 4. A recorded session, and the server key leaks later

The input: the request POST /transfer?from=ACC-1001&to=ACC-2002&amount=2500 is sent once under each of three TLS 1.2 suites, through a tap that records both directions. In no recording is the request visible in clear.
What the client sent in its key exchange message: static RSA, 256 bytes, the secret encrypted to the server's key; DHE, 128 bytes, a DH public value; ECDHE, 32 bytes, an EC public value. The secrets of DHE and ECDHE were made from keys thrown away after the handshake.
How the leaked key opens the static RSA session: RSA-decrypt the 256 bytes to the 48-byte pre-master secret; the TLS 1.2 PRF with "master secret" and both randoms gives the 48-byte master secret; with "key expansion" a 40-byte key block; bytes 0 to 15 are the client write key and bytes 32 to 35 the salt; AES-GCM with the salt and 8 bytes from the record opens the request. The result: static RSA opened, DHE and ECDHE not, and a key that is not the server's opens 0 of 3.

### 5. What TLS 1.3 removed

The input is a server with a minimum of TLS 1.3 whose configuration still lists DHE-RSA-AES128-SHA and AES128-GCM-SHA256 and a 1024-bit DH group.
What a client asked for, and the answer: TLS 1.0, 1.1 and 1.2 refused 3 of 3 with the alert protocol version; static RSA (AES128-GCM-SHA256) and CBC with a MAC (AES128-SHA) do not connect; a TLS 1.3 hello started with -comp offers only method 0, no compression; the client library refuses to renegotiate, because TLS 1.3 forbids it; the 1024-bit DH group is not used, the named group X25519MLKEM768 is.
The TLS 1.3 suites, from openssl ciphers -v -tls1_3: TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256 and TLS_AES_128_GCM_SHA256, all Kx=any, Au=any, Mac=AEAD: 0 of 3 with a fixed key exchange, 0 of 3 not AEAD. For comparison, the weak server on TLS 1.2: no compression (this build has no zlib), renegotiation refused with a no_renegotiation alert, and no RC4 in this build.

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
