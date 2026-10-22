# TLS Servers in Practice with nginx and Caddy, Explained

## The idea in one sentence

A TLS server is a handful of choices, and each choice is either a line you wrote or a default someone else made.

## The 5 acts

### 1. nginx with a certificate from the local authority

nginx serves www.shop.localhost with a certificate signed by the lab's root. A client that trusts the root gets 200 over TLS 1.3. A client that trusts only its default roots refuses. The same client asking for pay.shop.localhost, which the certificate does not name, is refused. Plain http is answered with 301 to the https address. By default the Server header gives nginx's exact version; with server_tokens off it says only nginx.

### 2. Which protocol versions it speaks

The default accepts a client that offers only TLS 1.2, and one that offers 1.3. With ssl_protocols TLSv1.3, the client that offers only 1.2 is refused with no protocol version in common, and the one that offers 1.3 is served. The price of the stricter setting is every client that cannot speak 1.3; the setting is one line, and a change needs a reload.

### 3. HSTS, and the add_header trap

By default there is no Strict-Transport-Security header. An add_header in the server block sends it over https and not over plain http. A location, /account, with its own add_header (X-Frame-Options) replaces the server's headers: the page has X-Frame-Options and no HSTS. Repeating the line in the location brings it back. A browser that has seen the header refuses plain http to the name for the max-age.

### 4. Caddy makes its own certificates

The Caddyfile names only the site. Caddy made a root, an intermediate and a certificate by itself, issued by Caddy Local Authority; the certificate lasts 12 hours and Caddy renews it on its own. A client that trusts nginx's root but not Caddy's is refused. Plain http gets 308 to https. The Server header says only Caddy and there is no HSTS by default. A client that offers only TLS 1.2 is accepted by default. A header line in the Caddyfile adds HSTS.

### 5. The bill

With ssl_stapling on, a client that asks for a stapled OCSP answer receives none, and nginx says it found no responder URL in the certificate. Caddy's local certificates carry no responder either. A new certificate written over the old file is not served by nginx until nginx -s reload. nginx needs a renewal job that ends with a reload; Caddy swaps by itself and needs its data folder kept. Every client needs the root of whichever authority signed.

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
