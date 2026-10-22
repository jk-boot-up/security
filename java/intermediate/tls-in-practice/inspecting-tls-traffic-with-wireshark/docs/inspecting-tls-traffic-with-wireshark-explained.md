# Inspecting TLS Traffic with Wireshark, Explained

## The idea in one sentence

Encryption hides the content and not the conversation, and the secrets that open it are kept per connection by whoever wrote them down.

## The 5 acts

### 1. What the wire shows

A login to the shop over TLS 1.3 is copied by a tap and read with tshark and no secrets. The Client Hello names www.shop.localhost in the clear and the Server Hello chooses TLS_AES_256_GCM_SHA384. The certificate is not in the capture, no HTTP request can be read, and the encrypted application data records are there. The bytes of the capture do not contain the word password. A watcher still learns the name, the times and the sizes.

### 2. A key log: the secrets of one connection

openssl s_client -keylogfile writes five lines, as a browser does when SSLKEYLOGFILE is set: the handshake and traffic secrets of both sides and the exporter secret. Each line is a label, the connection's client random (32 bytes) and a secret (48 bytes), which ties it to one connection and nothing else. The file holds no private key.

### 3. The same capture, read with the key log

With the key log, tshark shows POST /login with the form fields user and password, ana and hunter2; the response 200 with the Set-Cookie header and the page; and the encrypted handshake opens too, so the certificate, for www.shop.localhost, is visible. In TLS 1.3 the certificate is encrypted, and the key log opens it.

### 4. A key log opens its own connection and no other

A second login read with the first login's key log has 0 HTTP packets; with its own key log it opens. One file with both logs opens both captures. A log with only the client's lines opens the requests and not the responses.

### 5. The server's own key is a different matter

Over TLS 1.2 the certificate is visible without any secret; over TLS 1.3 it is not. A recording made with ephemeral key exchange (ECDHE) stays closed when the server's private key leaks: 0 HTTP packets opened. A recording made with RSA key exchange opens, password and all: a leaked key opens everything recorded before. A key log is as good as the password it protects. Nothing here needed a network card.

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
