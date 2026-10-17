# The TLS Handshake with OpenSSL, Explained

## The idea in one sentence

A TLS setup is best checked by a second, independent implementation that negotiates the same protocol, refuses the same wrong names and untrusted certificates, and reads the same PEM files, provided that it is told to verify.

## The 5 acts

### 1. A real TLS server and client on this machine

The server listens on the loopback address and the client connects by the name shop.example.test. The handshake works and agrees TLS 1.3 with the suite TLS_AES_256_GCM_SHA384. The server answers the order. Two engines wired together in memory agree
the same suite, as in the plain project. Anything that speaks TLS can take the client's place, so the tests let openssl do it.

### 2. Giving the certificate and key to other programs

The certificate file starts with BEGIN CERTIFICATE and ends with END CERTIFICATE. The key file starts with BEGIN PRIVATE KEY. The certificate names CN=shop.example.test and its key is an elliptic curve key.
The key file is readable by its owner only. PEM is only Base64 of the same bytes, so openssl reads what Java wrote. The key file holds the private key in the clear, so it must be protected like a password.

### 3. Who is on the other end

With the shop's own certificate and the name shop.example.test, the handshake works. With an impostor that has the same name but a certificate the client does not trust, it does not: the client refuses. With the shop's real certificate but a request for
another.example.test, it does not: the name must match. openssl s_client checks the same two things when it is given the options for returning an error on verification and for checking the host name.

### 4. Versions: what each side offers

A client that offers only TLS 1.2 to a server that allows the defaults gets a handshake that works, and the agreed protocol is TLS 1.2. The same client against a server that allows only TLS 1.3 fails. A client that offers only TLS 1.1 fails: old versions are switched off.
openssl s_client with the options for TLS 1.2 and TLS 1.1 makes the same offers, and the tests check what it is told.

### 5. Checking from the outside, and the bill

To check a server with another program, run openssl s_client with the address, the server name, the trusted certificate file, the host name to verify, and the option to return an error on verification. To serve with another program, run openssl s_server with the address,
the certificate and the key. The bill: a second implementation catches mistakes in the first, but only the settings that you actually pass are checked. Without the option to return an error, openssl prints the problem and carries on,
so a script must check the exit status and the verification line. And a private key written to a file for another program is one more copy to protect.

## The verdict

Check every new TLS setup from outside with openssl or a scanner, switch on hostname verification and return-on-error, and keep an automated check afterwards. Treat any key you export for another program as a new secret.

## How to recognise this in code you did not write

- `openssl s_client -connect host:443` in troubleshooting guides.
- `-----BEGIN CERTIFICATE-----` files in web server configuration.
- Online TLS scanners.

## Where you have already met this

- `openssl s_client -connect host:443` in almost every TLS troubleshooting guide.
- `curl -v https://...`, which prints the same handshake information.
- Online TLS scanners that grade a site's protocols and ciphers.
- `.pem`, `.crt` and `.key` files in web server configuration.
