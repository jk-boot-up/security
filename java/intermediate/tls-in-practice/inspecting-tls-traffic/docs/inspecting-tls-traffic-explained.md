# Inspecting TLS Traffic with a Key Log, Explained

## The idea in one sentence

A TLS capture is unreadable without keys; a key log holds the traffic secrets of exactly the connections it lists and must stay out of production; and forward secrecy means a stolen long-term server key cannot open recorded ephemeral connections.

## The 5 acts

### 1. What the wire shows

The client sends a login request and the server answers. A capture holds the client and server random values (sent in the clear), the key shares, and two records of 70 and 52 bytes, each beginning with the record header 17 03 03: "application data", because TLS 1.3 hides the real content type inside the encrypted part.
Searching the bytes for "password" finds nothing, and the decryptor with no keys returns nothing. Lengths and timing are still visible, which is a limit of TLS.

### 2. Writing a key log

A client that supports key logging reads SSLKEYLOGFILE. Here it returns nothing when the variable is not set, the path in development, and refuses to start in production. The file has one line per direction: a label, the client random in hex, and the traffic secret in hex. The client random
names the connection, and the secret is what the record keys are derived from.

### 3. Reading the capture with the key log

The decryptor finds the lines for this capture's client random, derives the record keys and nonces from the secrets, and opens the records in order. The login request appears with its password, and the reply with the session cookie value.
This is exactly what a developer needs for debugging, and exactly what an attacker gets by finding the file.

### 4. A key log opens its own connections and no others

Each connection has its own random values and key shares, so its secrets are unrelated to every other connection's. Session one's log decrypts nothing in session two's capture. Adding session two's lines opens it. Giving the decryptor only the client line of session one
opens the client's request and not the server's reply, because the two directions have separate secrets.

### 5. Why the server's own key is a different matter

With static RSA key exchange, the client encrypts the secret it chose under the server's long-term public key. An attacker who recorded the connection last year and steals the server's private key today decrypts the premaster secret, derives the same keys, and reads the login.
With ephemeral key exchange (X25519) the long-term key only signs the handshake: the traffic secrets came from values that were thrown away, so the stolen key opens nothing. That is forward secrecy, and the reason static RSA is gone from TLS 1.3 and a finding in the previous project.

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
