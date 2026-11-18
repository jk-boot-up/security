# TLS Servers in Practice with nginx and Caddy, Explained

## The idea in one sentence

A TLS server is a handful of choices, and each choice is either a line you wrote or a default someone else made.

## The 5 acts

### 1. nginx with a certificate from the local authority

The certificate, made with the step command, names www.shop.localhost, is issued by Shop Lab Root and lasts 24 hours. Four nginx lines serve it: listen ... ssl, ssl_certificate and ssl_certificate_key.
Three clients connect. Each one makes two checks, redone by hand by ChainCheck: is the issuer one of my roots and does its signature verify, and is the name I asked for in the certificate's names? The client that trusts the lab's root and asks for www.shop.localhost passes both and gets 200 over TLSv1.3. The client with only the default roots fails the first check: no trusted root signed it. The client that asks for pay.shop.localhost passes the first and fails the second: the certificate is not for that name.
Plain http on its own port is answered with 301 and a Location on https. By default the Server header is nginx/<version>; with server_tokens off it is only nginx.

### 2. Which protocol versions it speaks

Two clients: the old one offers only TLSv1.2, the new one only TLSv1.3. Client and server agree on the highest version both speak, which Negotiation redoes by hand.
Before the change, the line is ssl_protocols TLSv1.2 TLSv1.3. The old client agrees on TLSv1.2 and the new one on TLSv1.3: both get 200.
After the change, ssl_protocols TLSv1.3, the old client has no version in common and is refused; the new one still gets 200 over TLSv1.3. The price of the stricter line is every client that cannot speak 1.3.

### 3. HSTS, and the add_header trap

Before the fix there is no Strict-Transport-Security header. One line in the server block, add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always, sends it over https; plain http only redirects and does not carry it. max-age is in seconds: 31536000 / 86400 = 365 days, and includeSubDomains covers every name under the host.
The trap: the /account location has an add_header of its own, X-Frame-Options DENY. nginx applies the server's add_header lines to a location only when the location has none (Headers.effective redoes the rule), so /account answers with X-Frame-Options and no HSTS. Repeating the line in the location brings it back.

### 4. Caddy makes its own certificates

The Caddyfile's site block is four lines: the name, bind 127.0.0.1, respond "the shop" and the closing brace; local_certs and the ports are global options. Caddy made a whole chain by itself: the certificate for www.shop.localhost, signed by Caddy Local Authority - ECC Intermediate, signed by Caddy Local Authority - <year> ECC Root. The server sends the first two; the root stays in Caddy's data folder.
The certificate's lifetime is not after minus not before: 43200 seconds, 12 hours, and Caddy renews it on its own.
A client that trusts Caddy's root gets 200 over TLSv1.3; one that trusts only the lab's root is refused; a client that offers only TLS 1.2 is served by default. Plain http gets 308, with a Location on https that leaves out the lab's https port. The headers say only Server: Caddy and carry no HSTS until a header line in the site block adds it.

### 5. The bill

Stapling means the server attaches a signed answer from the authority's OCSP responder, saying the certificate is not revoked. With ssl_stapling on and the root as ssl_trusted_certificate, the client receives 0 stapled answers: neither nginx's certificate nor Caddy's names a responder (the Authority Information Access extension is absent), and nginx's error log says "ssl_stapling" ignored, no OCSP responder URL in the certificate.
A new certificate is written over the old file. On disk it is the new one, but nginx still serves the old serial: it reads the file only when it starts or reloads. After nginx -s reload it serves the new one. A renewal job for nginx must end with a reload; Caddy renews and swaps by itself.

## The verdict

Serve only TLS 1.2 and 1.3 (1.3 where you can), send HSTS on every page, hide the version, renew automatically and reload, test the result as a client sees it, and know which of these your server decides for you.

## How to recognise this in code you did not write

- `ssl_certificate`, `ssl_protocols` and `add_header` in nginx.
- A Caddyfile with only a site name.
- `nginx -s reload` after a renewal.
- `Strict-Transport-Security` in a response.

## Where you have already met this

- `ssl_certificate` and `ssl_protocols` in an nginx configuration.
- A Caddyfile that is only a domain name and a `reverse_proxy` line.
- `add_header Strict-Transport-Security` copied from a blog post.
- A cron job that runs `nginx -s reload` after a renewal.
