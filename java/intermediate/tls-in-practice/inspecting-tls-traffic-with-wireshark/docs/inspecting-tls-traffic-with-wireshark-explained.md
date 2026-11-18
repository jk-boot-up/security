# Inspecting TLS Traffic with Wireshark, Explained

## The idea in one sentence

Encryption hides the content and not the conversation, and the secrets that open it are kept per connection by whoever wrote them down.

## The 5 acts

### 1. What the wire shows

The shopper's client sends POST /login with user=ana&password=hunter2, inside TLS 1.3. A tap copies the connection to login.pcap, and tshark reads it with no secrets. The Client Hello names www.shop.localhost and the Server Hello chooses TLS_AES_256_GCM_SHA384; the certificate is not visible, no HTTP request can be read, and the word password is not in the file's bytes.
ClientHello reads the first record by hand: record type 22 (handshake), message type 1 (Client Hello), a 32-byte random that is new for every connection, and the server_name extension, in the clear.
The size still shows: the request is 163 bytes; TLS 1.3 adds one content-type byte and a 16-byte AES-GCM tag, so its record is 163 + 1 + 16 = 180 bytes, which is exactly the largest record tshark finds from the client.

### 2. A key log: the secrets of one connection

openssl s_client -keylogfile keys.log writes five lines, as a browser does when SSLKEYLOGFILE is set. In TLS 1.3 each side has a handshake secret and a traffic secret: CLIENT_HANDSHAKE_TRAFFIC_SECRET, SERVER_HANDSHAKE_TRAFFIC_SECRET, CLIENT_TRAFFIC_SECRET_0 and SERVER_TRAFFIC_SECRET_0; EXPORTER_SECRET opens no traffic.
One line taken apart: the label, the client random (32 bytes, 64 hex digits) and the secret (48 bytes, 96 hex digits). The secret is as long as the suite's hash: TLS_AES_256_GCM_SHA384 uses SHA-384, and 384 / 8 = 48 bytes.
KeyLog checks by hand whose connection it is: every line carries the same client random, and it is the random from the Client Hello in the capture. There is no private key in the file.

### 3. The same capture, read with the key log

tshark -o tls.keylog_file:keys.log reads the same login.pcap. The request opens with CLIENT_TRAFFIC_SECRET_0: POST /login, user = ana, password = hunter2. The response opens with SERVER_TRAFFIC_SECRET_0: 200, welcome ana, and Set-Cookie: session=7f3a; Secure; HttpOnly.
The certificate opens with SERVER_HANDSHAKE_TRAFFIC_SECRET: it is for www.shop.localhost, issued by Shop Lab Root. In TLS 1.3 the certificate is encrypted, so without the log it was not visible at all.

### 4. A key log opens its own connection and no other

A second login writes login2.pcap and keys2.log. KeyLog matches by hand: keys.log does not carry login2's client random, keys2.log does, and both.log (the two logs in one file) does.
tshark agrees: keys.log on login2.pcap opens 0 HTTP packets, keys2.log opens it, and both.log opens both captures.
Before: with all the lines of keys.log, requests and responses open. After: with only its two CLIENT_ lines, the requests open and the responses stay closed, because each side encrypts with its own secret.

### 5. The server's own key is a different matter

Three recordings and the suite each Server Hello chose: TLS 1.3 nginx with TLS_AES_256_GCM_SHA384, TLS 1.2 nginx with TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, and an old-style TLS 1.2 server (LegacyServer, in the JDK) with TLS_RSA_WITH_AES_128_CBC_SHA. Over TLS 1.2 the certificate is visible with no secret; over TLS 1.3 it is not.
Suites reads the key exchange from the name. TLS 1.3 is always ephemeral. ECDHE: both sides make a one-time key and the server's key only signs, so the secret never crosses the wire. RSA: the client sends the secret locked to the server's public key. So by hand, a leaked server key opens the RSA recording and nothing of the ECDHE one.
tshark with the server's private key (rsa.key) agrees: the ECDHE recording stays closed (0 HTTP packets) and the RSA recording opens, password hunter2. A leaked key opens everything ever recorded with RSA key exchange.

## The verdict

Keep key logs out of production and delete them, prefer ephemeral key exchange so that a leaked server key opens nothing old, remember that names, times and sizes stay visible, and treat a capture as sensitive even if it is encrypted.

## How to recognise this in code you did not write

- `SSLKEYLOGFILE` in an environment.
- A `.pcap` file in a ticket.
- A Wireshark screen of `Application Data` records.
- `tls.keylog_file` in a Wireshark preference.

## Where you have already met this

- An environment variable `SSLKEYLOGFILE` in a developer's shell.
- A Wireshark screen full of `Application Data`.
- A `.pcap` attached to a bug report.
- Someone saying "just turn off TLS in test".
