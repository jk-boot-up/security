# HTTPS Server with nginx, Explained

## The idea in one sentence

nginx can end TLS at the front door of a site with a short list of settings, but each setting has a trap, so the configuration should be tested and scored, and the application must be told what nginx saw and never what the client claims.

## The 5 acts

### 1. nginx ends the TLS for the shop

A Java client that trusts the demo root asks https://shop.example.test/ and gets 200 over TLS 1.3. HTTP/2 was agreed during the handshake. The five security headers score 5 of 5, and the Server header says just nginx, with no version. Behind nginx the application, on a plain local port,
answered: app saw proto=https. nginx told it what it saw.

### 2. Which TLS versions the shop allows

nginx set to TLS 1.3 only refuses a client limited to TLS 1.2, and a TLS 1.3 client gets TLS 1.3. With TLS 1.2 allowed as well, with three strong ciphers, the TLS 1.2 client gets TLS 1.2, and a TLS 1.3 client on the same server still gets TLS 1.3.
Allow TLS 1.2 only while customers still need it, and keep the list of ciphers short.

### 3. The plain port, the redirect and HSTS

A request to http://shop.example.test/orders?id=7 gets 301 and a Location of the same address on https. The plain answer carries no HSTS, because a browser ignores HSTS sent over plain HTTP. The HTTPS answer carries max-age=63072000, two years.
The very first visit is still plain, so HSTS protects every visit after it, and preloading protects the first.

### 4. Who the client trusts, and who the server trusts

The JDK's own list of trusted roots does not know the demo root, so it refuses. With the demo root trusted and the right name, the connection is accepted. Connecting by the address 127.0.0.1 is refused, because the name in the certificate must match.
A server with the right name and a certificate signed by nobody trusted is refused. With ssl_verify_client on, a client with no certificate is refused, and with Ana's certificate the answer is 200 and the application is told Ana's name.
When Ana adds a header claiming to be the admin, nginx overwrites it, because it sets that header itself.

### 5. The proxy trap, a config check and a score

If nginx passes on the client's own X-Forwarded-Proto, a plain request that claims https makes the application believe the connection is secure. When nginx sets the header from what it saw, the application is told http.
nginx -t accepts the careful configuration and refuses one whose certificate file is missing, before any customer sees it. The careful configuration scores 7 of 7 on the TLS habits and a typical copied configuration scores 1 of 7.
OCSP stapling is for certificates from a public authority and needs the server to reach it, so it is described and not run here.

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
