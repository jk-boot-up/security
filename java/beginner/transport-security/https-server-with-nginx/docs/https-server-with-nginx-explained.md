# HTTPS Server with nginx, Explained

## The idea in one sentence

nginx can end TLS at the front door of a site with a short list of settings, but each setting has a trap, so the configuration should be tested and scored, and the application must be told what nginx saw and never what the client claims.

## The 5 acts

### 1. nginx ends the TLS for the shop

The HTTPS server block of nginx, in short: listen 443 ssl; http2 on; ssl_certificate /pki/shop.crt; ssl_protocols TLSv1.3; and proxy_pass http://127.0.0.1:8081, the application on a plain local port. A Java client that trusts the demo root asks GET https://shop.example.test/.
The response is 200 over TLSv1.3, with HTTP/2 agreed in the handshake, the five security headers (1 + 1 + 1 + 1 + 1 = 5 of 5) and Server: nginx with no version. The app behind nginx answers app saw proto=https client=: it was told by nginx that the customer used HTTPS.

### 2. Which TLS versions the shop allows

Two configurations. Strict: ssl_protocols TLSv1.3;. Compat: ssl_protocols TLSv1.2 TLSv1.3; with three ciphers for TLS 1.2, ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES256-GCM-SHA384 and ECDHE-ECDSA-CHACHA20-POLY1305.
Each cipher name lists its parts: ECDHE agrees a fresh key, ECDSA signs with the certificate's key, AES128-GCM (or AES256-GCM, or CHACHA20-POLY1305) encrypts, and SHA256 or SHA384 is the hash. A client that offers only TLS 1.2 is refused by the strict server and gets TLSv1.2 from the compat one; a TLS 1.3 client gets TLSv1.3 from both.
Allow TLS 1.2 only while customers still need it, and keep the list of ciphers short.

### 3. The plain port, the redirect and HSTS

A request to the plain port, GET http://shop.example.test/orders?id=7, answers 301 with Location: https://shop.example.test/orders?id=7, the same path and query on HTTPS, and no Strict-Transport-Security, which a browser ignores over plain HTTP.
The HTTPS answer carries Strict-Transport-Security: max-age=63072000; includeSubDomains, and 63072000 / 86400 seconds a day = 730 days, two years. The very first visit is still plain: HSTS protects every visit after it, and preloading protects the first.

### 4. Who the client trusts, and who the server trusts

The client checks the server: (a) with the JDK's own trusted roots: refused, the demo root is not in the JDK's list; (b) with the demo root trusted and the name shop.example.test: accepted; (c) the same, connecting by the address 127.0.0.1: refused, the certificate names shop.example.test;
(d) an impostor with the right name, signed by nobody trusted: refused. Then the server checks the client, with ssl_verify_client on: (e) no certificate: refused; (f) Ana's certificate: 200, and the app is told proto=https client=CN=Ana the customer;
(g) Ana's certificate and a forged X-Client-DN: CN=Admin header: 200, and the app is still told client=CN=Ana the customer, because nginx sets X-Client-DN itself and drops Ana's.

### 5. The proxy trap, a config check and a score

A plain HTTP request sends GET /app with a forged X-Forwarded-Proto: https. When nginx copies the client's header, the app is told proto=https and is fooled into believing the connection is secure; when nginx sets the header from $scheme, what it saw itself, the app is told proto=http.
nginx -t checks a configuration before any customer sees it: the careful one gives syntax ok, and one whose certificate file is missing is refused. Seven TLS habits are scored: modern protocols only, HSTS on every answer, plain port redirects, session tickets off, version hidden, HTTP/2, and the app told what nginx saw.
The careful configuration scores 1 + 1 + 1 + 1 + 1 + 1 + 1 = 7 of 7; a typical copied one scores 0 + 0 + 0 + 0 + 1 + 0 + 0 = 1 of 7. OCSP stapling needs a public authority, so it is described and not run.

## The verdict

Use TLS 1.3, add 1.2 only if customers need it, send HSTS with always, redirect the plain port, hide the version, and set forwarded headers from nginx's own variables. Test every configuration with nginx -t, and keep private keys out of repositories.

## How to recognise this in code you did not write

- `ssl_protocols`, `ssl_ciphers` and `ssl_session_tickets` in nginx configuration.
- Cloud load balancers listed as "TLS termination".
- `X-Forwarded-Proto` handling in framework settings, such as "trust the proxy".
- Mozilla's SSL configuration generator.

## Where you have already met this

- `ssl_protocols`, `ssl_certificate` and `ssl_certificate_key` in every nginx tutorial.
- Mozilla's SSL configuration generator, and SSL Labs' test of a site.
- `X-Forwarded-For` and `X-Forwarded-Proto` headers in application logs.
- Load balancers that "terminate TLS" in cloud diagrams.
