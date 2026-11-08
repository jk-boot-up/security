# The TLS Handshake, Explained

## The idea in one sentence

TLS lets a client and a server agree on fresh secret keys in public, lets the client check the server's certificate and name, and then protects every record against reading and tampering, while the site's name and the sizes of messages stay visible.

## The 5 acts

### 1. A sniffer on the wire, without and with TLS

Before TLS, the order card=4111111111111111&order=A-1001 crosses the wire as it is: 34 bytes, and the sniffer reads the card number. After TLS, the sniffer sees one record of 72 bytes. Its 5-byte header is in the clear: 17 is record type 23, application data;
03 03 is a version label kept for old network boxes; 00 43 says 67 bytes follow. Those 67 bytes are the 34 bytes of the order, encrypted, 1 byte for the real record type, 16 bytes of zero padding that the JDK adds, and the 16-byte authentication tag. 5 + 34 + 1 + 16 + 16 = 72, and the card number is no longer readable.

### 2. The handshake

Before the keys exist, two hellos cross the wire in the clear, both handshake records of type 22. The ClientHello lists the versions and ciphers the client can use, a temporary public key and the site name, shop.example.test, which the sniffer can read. The ServerHello chooses TLSv1.3 and TLS_AES_256_GCM_SHA384 and adds the server's temporary public key.
Each side mixes its own temporary private key with the other's public key and gets the same secret, so the keys are never sent. After that, the client sends records of types 22 20 23 and the server 22 20 23 23: type 20, change cipher spec, is kept only for old network boxes, and type 23 is encrypted, so the certificate is not readable.
The cipher suite, part by part: TLS, the protocol; AES with a 256-bit key, the cipher; GCM, the mode that encrypts and adds a tag; SHA384, the hash that makes the keys.

### 3. Who is on the other end

Before the handshake the client knows two things: the name it asks for, shop.example.test, and the one certificate it trusts, whose SHA-256 fingerprint starts 0ded3842d38b01a7. After the handshake, three servers have answered. The shop presents that certificate, and the name matches: it works.
An impostor presents a certificate with the same name but another fingerprint, b72122451fcf2531, which the client does not trust: refused. The shop again, but the client asked for other.example.test: the certificate is trusted, but the name does not match, so it is refused too.
A name alone proves nothing; a certificate proves a key belongs to a name only because someone the client trusts vouched for it.

### 4. Protecting the data

Before sending, the order. After sending, the same order in two connections gives different bytes on the wire, because each connection has fresh keys, and untouched the server opens the record and gets the order back.
Before the check, an attacker flips one bit, byte 36 of 72, xor 01. After the check, the server, which recomputes the tag with its key, refuses the record: (bad_record_mac) Tag mismatch. So TLS protects integrity as well as secrecy.
Because the keys of each connection come from temporary key pairs, a stolen server key cannot open recorded traffic. This is called forward secrecy.

### 5. Settings, and the bill

Before tightening, against a default server: a client that offers only TLSv1 is refused, because old versions are switched off, and a client that offers only TLSv1.2 connects and agrees TLSv1.2. After tightening, the server requires TLS 1.3: the TLSv1.2 client is refused and a TLSv1.3 client connects.
The bill: certificates must be obtained, renewed and kept safe, the handshake costs a round trip and some computing, TLS protects the connection but not the ends, and the site name and the message sizes still show.

## The verdict

Use TLS 1.3, or 1.2 where needed, for every connection. Keep old versions off. Let the client verify the certificate and the name. Renew certificates automatically and protect their private keys. Remember what TLS does not hide.

## How to recognise this in code you did not write

- `https://` and the padlock in a browser.
- "Your connection is not private" errors for expired or wrong-name certificates.
- `minimum_tls_version` in load balancer settings.
- `curl -v` output showing the ClientHello, the ServerHello and the cipher suite.

## Where you have already met this

- The padlock in a browser, and `https://` in every address.
- "Certificate expired" and "your connection is not private" errors.
- `curl -v https://...` showing the handshake and the negotiated cipher.
- Settings called `minimum TLS version` in cloud load balancers.
