# HTTP Basics and Security Headers with nginx, Explained

## The idea in one sentence

nginx can add the security headers and refuse ambiguous requests before the application sees them, as long as every header carries always, every location is audited for inheritance, and what the application reveals is removed or, better, not revealed.

## The 5 acts

### 1. nginx in front of the shop, as it comes

The online store puts nginx at its front door, as it comes. A request GET / is answered with status 200. The response carries Server: nginx/1.31.7, the exact version, and none of the five security headers, so the audit adds 0 + 0 + 0 + 0 + 0 = 0 of 5.
The version number is a free hint: an attacker looks up the known holes of exactly that release.

### 2. add_header, and the word always

nginx adds a header with add_header, for example add_header X-Frame-Options "DENY";. The store adds the five security headers this way: Strict-Transport-Security max-age=31536000; includeSubDomains, Content-Security-Policy default-src 'self', X-Content-Type-Options nosniff, X-Frame-Options DENY and Referrer-Policy no-referrer.
Without the word always, nginx adds them only to answers such as 200, 201, 204, 206, 301, 302, 303, 304, 307 and 308: GET / scores 5 of 5, but GET /missing, a 404 page, scores 0 + 0 + 0 + 0 + 0 = 0 of 5. With always on each line both pages score 1 + 1 + 1 + 1 + 1 = 5 of 5.
With server_tokens off, the Server header is just nginx, without the version.

### 3. The inheritance trap

The five headers are set once, in the server block, and GET / scores 1 + 1 + 1 + 1 + 1 = 5 of 5. The /account location adds one header of its own, add_header Cache-Control "no-store" always;, so account pages are not kept in a cache.
nginx inherits add_header from the level above only when a level has no add_header of its own, so /account now has Cache-Control: no-store and none of the five: 0 + 0 + 0 + 0 + 0 = 0 of 5. With the five repeated inside /account, it scores 5 of 5 again.
The habit: audit every location, not just the top page.

### 4. Limits, and requests that are ambiguous

Two lines set the limits: client_max_body_size 1k; and large_client_header_buffers 2 1k;, where 1k is 1024 bytes. Five requests are sent as raw bytes. An order with a 100-byte body gets 200, because 100 <= 1024. One with a 2000-byte body gets 413, because 2000 > 1024.
A header line of 4007 characters (X-Pad: and 4000 x) gets 400, because a header line must fit one 1024-byte buffer. A request with Content-Length: 4 and Transfer-Encoding: chunked, two ways to say where the body ends, gets 400, and so does one with Content-Length: 4 and Content-Length: 5.
nginx refuses both ambiguous requests, so it cannot disagree with the application behind it about where a request ends, which is the idea behind request smuggling.

### 5. Hiding what the application gives away, and the bill

The application behind nginx sends X-Powered-By: PHP/5.6.40 and X-AspNet-Version: 4.0.30319, and nginx itself says Server: nginx/1.31.7: three exact versions, and old versions have known holes. With proxy_hide_header for the two application headers and server_tokens off, both are hidden and Server says just nginx.
Hiding is a speed bump, not a lock: the real fix is to keep the software up to date. And the bill: the configuration is easy to get subtly wrong, so test it, and a Content-Security-Policy still has to match the real site.

## The verdict

Set the five headers with always, turn server_tokens off, repeat the headers (or include a snippet) in every location that adds its own, set sane size limits, and test the result with an auditor against every location and an error page.

## How to recognise this in code you did not write

- `add_header ... always;` lines in nginx configurations.
- A letter grade from a header scanner.
- `proxy_hide_header X-Powered-By;`.
- `client_max_body_size` in an upload error report.

## Where you have already met this

- `add_header` lines in almost every nginx configuration, and `always` copied from a snippet nobody explained.
- Scanners such as securityheaders.com that give a site a letter grade.
- Include files such as `security-headers.conf` repeated in each `location`.
- Advisories about request smuggling between a proxy and an application server.
