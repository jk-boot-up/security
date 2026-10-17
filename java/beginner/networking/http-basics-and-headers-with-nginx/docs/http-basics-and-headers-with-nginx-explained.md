# HTTP Basics and Security Headers with nginx, Explained

## The idea in one sentence

nginx can add the security headers and refuse ambiguous requests before the application sees them, as long as every header carries always, every location is audited for inheritance, and what the application reveals is removed or, better, not revealed.

## The 5 acts

### 1. nginx in front of the shop, as it comes

A request for the home page is answered 200 and scores 0 of 5 on the five security headers. The Server header shows the exact nginx version, a free hint for an attacker who wants to look up its known problems.

### 2. add_header, and the word always

With the five headers added without always, the home page scores 5 of 5, but the 404 page scores 0 of 5: error pages are left bare. With always, both score 5 of 5. With server_tokens off the Server header is just nginx.

### 3. The inheritance trap

With the headers set once for the whole server, the home page scores 5 of 5. But /account adds its own Cache-Control header, and now scores 0 of 5: its own add_header dropped the rest. Cache-Control is there, the five security headers are not.
Repeating the five headers inside /account brings it back to 5 of 5. The habit is to audit every location, not just the top of the page.

### 4. Limits, and requests that are ambiguous

With client_max_body_size 1k, a 100 byte order gets 200 and a 2000 byte order gets 413. With large_client_header_buffers 2 1k, a header line of 4000 characters gets 400.
A request with both Content-Length and Transfer-Encoding, the smuggling pattern, gets 400, and so do two different Content-Length headers. nginx refuses both, so it cannot disagree with the application behind it about where a request ends.

### 5. Hiding what the application gives away, and the bill

The application behind the proxy sends X-Powered-By and X-AspNet-Version, and the proxy passes them on: old versions have known holes. With proxy_hide_header and server_tokens off, no extra headers are left and the Server header says just nginx.
Hiding is a speed bump, not a lock: the real fix is to keep the software up to date. The bill: the configuration is easy to get subtly wrong, so it has to be tested, and the Content-Security-Policy has to match the real site.

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
