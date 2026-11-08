# The TLS Handshake with OpenSSL, Explained

## The idea in one sentence

A TLS setup is best checked by a second, independent implementation that negotiates the same protocol, refuses the same wrong names and untrusted certificates, and reads the same PEM files, provided that it is told to verify.

## The 5 acts

### 1. A real TLS server and client on this machine

The request: a Java client connects over a real socket to the name shop.example.test at the loopback address 127.0.0.1, and sends order=A-1001. The response, after the handshake: it works, the protocol is TLSv1.3, the cipher suite TLS_AES_256_GCM_SHA384, and the server's reply is received: order=A-1001.
Two engines wired together in memory agree the same suite. Anything that speaks TLS can take either side, so the integration tests put the openssl program in the client's place and in the server's.

### 2. Giving the certificate and key to other programs

Java holds the certificate as DER bytes: 371 of them, starting 30 82 01. Other programs read PEM text, made in two steps. Step 1, Base64: three bytes are 24 bits, 00110000 10000010 00000001, cut into four groups of six bits, 001100 001000 001000 000001, which are the numbers 12 8 8 1, which are the letters M I I B in the Base64 alphabet (0 = A, 25 = Z, 26 = a, 52 = 0, 63 = /).
Step 2: the 371 bytes become 496 letters, written in lines of 64 between -----BEGIN CERTIFICATE----- and -----END CERTIFICATE-----, a file of 10 lines. The key file is made the same way, between PRIVATE KEY labels, for CN=shop.example.test with an EC key, with permissions rw------- so only its owner can read it. It holds the private key in the clear, so protect it like a password.

### 3. Who is on the other end

The request: the client asks for shop.example.test and trusts one certificate, whose SHA-256 fingerprint starts 0ded3842d38b01a7. The response, from three servers over real sockets: the shop works; an impostor with the same name but a certificate with fingerprint b72122451fcf2531, which the client does not trust, is refused;
and the shop itself is refused when the client asked for another.example.test, because the name must match. openssl s_client makes the same two checks when it is given -CAfile, -verify_hostname NAME and -verify_return_error.

### 4. Versions: what each side offers

Before tightening, against a server with the JDK's defaults: a client that offers only TLSv1.1 is refused, because old versions are switched off, and a client that offers only TLSv1.2 agrees TLSv1.2. After tightening, the server allows only TLS 1.3: the TLSv1.2 client is refused and a TLSv1.3 client works.
openssl s_client -tls1_1, -tls1_2 and -tls1_3 make the same offers, and the integration tests check what openssl is told.

### 5. Checking from the outside, and the bill

To check a server from the outside, the request is the command openssl s_client -connect HOST:PORT -servername NAME -CAfile cert.pem -verify_hostname NAME -verify_return_error -brief. The response the tests check, for this server: Protocol version: TLSv1.3, Ciphersuite: TLS_AES_256_GCM_SHA384, Verification: OK, and the reply received: order=A-1001.
To serve from openssl instead: openssl s_server -cert cert.pem -key key.pem. The bill: a second program catches mistakes in the first, but only the settings you pass are checked; without -verify_return_error, openssl reports a problem and goes on, so a script must check the exit status; and a key file for another program is one more copy to protect.

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
