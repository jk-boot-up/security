# Inspecting TLS Traffic with a Key Log, Explained

## The idea in one sentence

A TLS capture is unreadable without keys; a key log holds the traffic secrets of exactly the connections it lists and must stay out of production; and forward secrecy means a stolen long-term server key cannot open recorded ephemeral connections.

## The 5 acts

### 1. What the wire shows

The input is one login to an online store: the client sends "POST /login HTTP/1.1 | user=ana&password=hunter2" and the server answers "HTTP/1.1 200 OK | session=7f3a".
The capture holds the client random (3bd68db307915645...) and the server random (363791a138986206...) in the clear, then a 70-byte record from the client and a 52-byte record from the server.
The record header, worked out: 17 is type 23, application data; 03 03 is version 3.3, the same in every TLS 1.3 record; 00 41 is a length of 65 = 48 bytes of text + 1 inner content type + 16 bytes of GCM tag; 5 header bytes + 65 = 70.
The result with no key: the word "password" is not in the bytes, and nothing decrypts.

### 2. Writing a key log

The input is the client's environment. Without SSLKEYLOGFILE there is no key log; with SSLKEYLOGFILE=/tmp/keys.log and APP_ENV=dev the client writes to /tmp/keys.log; with APP_ENV=production it refuses to start.
The result is two lines in the NSS key log format: a label, the client random and the secret. The line part by part: the label CLIENT_TRAFFIC_SECRET_0 names the client-to-server direction, the client random 3bd68db30791... (32 bytes) is the same as the capture's, and it is how a tool matches the line to a connection, and the secret 7c6da57caa88... (32 bytes) opens that one direction.

### 3. Reading the capture with the key log

The input is the capture and the key log. The capture's client random 3bd68db30791... is looked up in the log, and both lines are found: CLIENT_TRAFFIC_SECRET_0 7c6da57caa88... and SERVER_TRAFFIC_SECRET_0 ea2a4ed1b2fa....
From secret to key, worked out for the client direction: the key is HKDF-Expand-Label(secret, "key", 16) = cfeb456056d9d85d..., the IV is HKDF-Expand-Label(secret, "iv", 12) = 6d3d06a138e98f67..., and the nonce of each record is the IV XOR the record number, so record 0 ends db a7 and record 1 would end db a6. AES-128-GCM opens the record and checks the header with it.
The result: both records open, with the login, the password and the session.

### 4. A key log opens its own connections and no others

The input is a second connection, another customer asking for GET /orders, with client random 1ccd6b949daf... where session one has 3bd68db30791....
Try 1, session two with session one's key log: no line has its client random, so nothing opens. Try 2, with its own two lines added: client "GET /orders HTTP/1.1" and server "HTTP/1.1 200 OK | 3 orders" open. Try 3, session one with only its CLIENT line: the client's login opens, the server's record stays closed.
The result: a key log line opens one direction of one connection, no more.

### 5. Why the server's own key is a different matter

The input is two recordings from last year. With static RSA key exchange the client's secret crossed the wire encrypted to the server's long-term key, 256 bytes. With ECDHE only two public values of 32 bytes crossed (8d0e655ac863... and 2af6fb559353...); the shared secret was never sent. Today the server's long-term RSA private key leaks.
The result: the static RSA recording opens, login and password included; the ECDHE recording opens nothing.
Why, worked out: with static RSA the key decrypts the 256 bytes and gets the secret; with ECDHE the secret needs a private value of the handshake, thrown away when it ended.

## The verdict

Use key logs only for your own traffic in development and delete them after use. Make the setting fail loudly in production. Offer only ephemeral key exchange (TLS 1.3, or ECDHE in TLS 1.2), so that a future key theft does not unlock recordings.

## How to recognise this in code you did not write

- `SSLKEYLOGFILE` in an environment, a Dockerfile or a run script.
- Lines starting `CLIENT_RANDOM`, `CLIENT_TRAFFIC_SECRET_0` or `SERVER_TRAFFIC_SECRET_0`.
- Wireshark's TLS protocol preference "(Pre)-Master-Secret log filename".
- A finding about private keys or secrets written to log files, CWE-532.

## Where you have already met this

- Wireshark's "Pre-Master-Secret log filename" setting.
- `SSLKEYLOGFILE=/tmp/keys.log` before starting a browser or `curl`.
- Java's `-Djavax.net.debug=ssl` output, which prints handshake detail (and in some versions secrets).
- The advice to turn off static RSA cipher suites.
