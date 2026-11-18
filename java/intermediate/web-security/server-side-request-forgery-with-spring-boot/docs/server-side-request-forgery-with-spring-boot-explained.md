# Server-Side Request Forgery with Spring Boot, Explained

## The idea in one sentence

A server that fetches an address for a customer lends out its network position, so the address that is really connected to must be judged, after resolution, and the connection must go to that same address.

## The 6 acts

### 1. The shop's import form works

The invented internet: images.example at 203.0.113.10 (a public image host), evil.example at 203.0.113.66 (the attacker's host), the cloud metadata service at 169.254.169.254, the shop's own settings on localhost (127.0.0.1) and an office admin page at 10.0.0.5. Each is a real HTTP server on the loopback address.
The staff form posts POST /import with the body url=http%3A%2F%2Fimages.example%2Fcat.png. Spring decodes it: %2F is hex 2F = 47, the character '/', and %3A is hex 3A = 58, the character ':'. The value is http://images.example/cat.png.
The importer's log: look up images.example: [203.0.113.10]; GET /cat.png at 203.0.113.10, Host: images.example; 200 image/png, 7 bytes. The shop answers 200 imported: PNG-cat.

### 2. The server fetches whatever it is told

The naive importer fetches http://169.254.169.254/latest/meta-data/, http://localhost/ and http://10.0.0.5/. The shop answers 200 to each: AccessKeyId=DEMO-NOT-A-REAL-KEY, DB_PASSWORD=demo-not-real and internal admin panel: 3 customers.
Its log for the first shows nothing stopped it: look up 169.254.169.254, GET /latest/meta-data/ at 169.254.169.254, 200 text/plain, 31 bytes.

### 3. A blocklist of names is easy to walk around

The blocklist compares the host, as text, with 169.254.169.254, localhost, 127.0.0.1 and 0.0.0.0. Only request a is refused, with 400 refused: blocked host name 169.254.169.254.
Java connects b, 2852039166, to 169.254.169.254; c, 127.1, to 127.0.0.1; d, ::ffff:a9fe:a9fe, to 169.254.169.254; e, inside.evil.example, to 10.0.0.5; and f, evil.example, to 203.0.113.66, which answers 302 and sends the importer to 169.254.169.254.
The decimal spelling is the address in base 256: 169 x 16777216 + 254 x 65536 + 169 x 256 + 254 x 1 = 2852039166.

### 4. Check the address the connection really uses

For request f the guarded importer logs: port 80, no user info: allowed; look up evil.example once: [203.0.113.66]; 203.0.113.66 is public: connect to it; GET /redirect at 203.0.113.66, Host: evil.example; 302 redirect to 169.254.169.254: check it again; the same checks; refused: 169.254.169.254 is not public. The shop answers 400.
Every other attack is refused by the address it resolves to: 2852039166 is 169.254.169.254, 127.1 is 127.0.0.1, inside.evil.example is 10.0.0.5, localhost is 127.0.0.1. images.example@169.254.169.254 is refused for its user info.
dual.evil.example answers two addresses, 203.0.113.66 and 10.0.0.5; one private answer refuses the name. The honest image still imports with 200.

### 5. Limits on what is fetched

The limits come from the code: GuardedImporter.PORTS is 80 and 443, the content type must start with image/, Fetcher.MAX_BYTES is 1024 and Fetcher.TIMEOUT is 1500 ms.
http://images.example:22/ is refused: port 22 is not allowed. http://evil.example/page.html is refused: not an image (text/html). http://evil.example/big.png is refused: larger than 1024 bytes. http://evil.example/slow.png, which answers after three seconds, is refused: no answer within 1500 ms.

### 6. A second lookup can give a different answer

The attacker's name server answers rebind.evil.example with 203.0.113.66 the first time and 10.0.0.5 after that (DNS rebinding).
The two-step importer logs: check: look up rebind.evil.example: [203.0.113.66], public; look up rebind.evil.example: [10.0.0.5]; GET /cat.png at 10.0.0.5. The shop answers 200 imported: internal admin panel: 3 customers. The check was right; the connection went elsewhere.
The guarded importer looks the name up once, checks 203.0.113.66, and connects to 203.0.113.66: 200 imported: PNG-attacker-cat. Nothing inside is reached.

## The verdict

Resolve once, judge every address, connect to the judged address, revalidate every redirect, allow only the ports and content you need, cap size and time, prefer an allow-list of hosts where you have one, and add network egress rules as a second wall.

## How to recognise this in code you did not write

- `RestClient.get().uri(url)` with a user-supplied URL.
- `isSiteLocalAddress()` checks in validation helpers.
- A cloud metadata address in an incident report.
- "Import from URL" in an admin form.

## Where you have already met this

- `RestClient.get().uri(url)` with a URL from a request parameter.
- A cloud metadata address in an incident report.
- `InetAddress.isSiteLocalAddress()` in a validation helper.
- A webhook or image import form that takes a URL.
