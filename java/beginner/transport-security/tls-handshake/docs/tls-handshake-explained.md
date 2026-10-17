# The TLS Handshake, Explained

## The idea in one sentence

TLS lets a client and a server agree on fresh secret keys in public, lets the client check the server's certificate and name, and then protects every record against reading and tampering, while the site's name and the sizes of messages stay visible.

## The 5 acts

### 1. A sniffer on the wire, without and with TLS

Without TLS the order is sent as it is, and the sniffer sees the card number. With TLS the sniffer sees a record of type 23, application data, and no card number. The record is only a few dozen bytes larger than the order: a header, padding and an authentication tag.

### 2. The handshake

The client speaks first, with a handshake record called ClientHello. It is not encrypted, because no key exists yet, so the sniffer reads the name of the site. The server answers with a ServerHello, which chooses the protocol and cipher: TLS 1.3 and TLS_AES_256_GCM_SHA384.
After the ServerHello the rest of the handshake is encrypted, so even the certificate is hidden from the sniffer. Both sides now share keys that were never sent over the wire.

### 3. Who is on the other end

The shop's own certificate, for the name shop.example.test, works. An impostor with the same name but a certificate the client does not trust is refused by the client. The shop's real certificate, when the client asked for another name, is also refused: the name must match.
A certificate proves a key belongs to a name because someone the client trusts vouched for it.

### 4. Protecting the data

The server opens an untouched record and gets the order back. The same order in two connections gives different bytes on the wire, because each connection has fresh keys. One changed bit in a record is refused, so TLS protects integrity as well as secrecy.
Because the keys of each connection are made fresh from temporary key pairs, a stolen server key cannot open recorded traffic. This is called forward secrecy.

### 5. Settings, and the bill

A client that only offers TLS 1.0 cannot connect, because old versions are switched off. A client that offers only TLS 1.2 still works and agrees TLS 1.2, but fails against a server that requires TLS 1.3. The bill: certificates must be obtained and renewed,
the handshake costs a round trip and some computing, and the keys must be kept safe. TLS protects the connection between two ends, not what the ends do with the data, and the name of the site and the sizes of messages still show.

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
