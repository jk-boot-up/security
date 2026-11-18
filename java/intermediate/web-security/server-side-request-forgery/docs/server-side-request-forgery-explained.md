# Server-Side Request Forgery (SSRF) and Its Defences, Explained

## The idea in one sentence

When a server fetches an address for a customer, it must test the address the connection will really use, connect to exactly that address, and test every redirect again.

## The 5 acts

### 1. An honest import works

The store's server can reach five hosts: images.example at 93.184.216.34 (a public image host), attacker.example at 203.0.113.9, the cloud metadata service at 169.254.169.254, the server itself at 127.0.0.1 (localhost) and an office admin page at 10.0.0.5.
The seller pastes http://images.example/cat.png. The server looks up images.example, gets 93.184.216.34, and sends GET /cat.png to that address. The response is imported: PNG-cat.
This is the feature working as intended: the server, not the customer, makes the request.

### 2. The server fetches whatever it is told

The seller now pastes three other addresses. http://169.254.169.254/latest/meta-data/iam/credentials returns AccessKeyId=DEMO-NOT-A-REAL-KEY, the server's cloud key.
http://localhost/actuator/env returns DB_PASSWORD=demo-not-real, the server's own settings. http://10.0.0.5/admin returns internal admin panel: 3 customers.
The customer's own computer cannot reach any of them. The server can, and the import shows what it fetched.

### 3. A blocklist of names is easy to walk around

The blocklist compares the host, as text, with localhost, 127.0.0.1, 169.254.169.254, 0.0.0.0, 10.* and 192.168.*. It refuses request a, the metadata address as written. The other five get through.
b, 2852039166, is the whole address as one number: dividing by 16777216, 65536 and 256 gives 169, 254, 169 and 254. c, 127.1, has two parts, so the last number fills the last three parts: 127.0.0.1.
d, ::ffff:a9fe:a9fe, is an IPv4 address inside IPv6: hex a9 is 169 and fe is 254. e is a name whose DNS answer is 10.0.0.5, and f is a public host that answers 302 and sends the server to 169.254.169.254 after the check.
Java reads each spelling exactly as the worked steps do. The blocklist read the text; the connection used the address.

### 4. Check the address the connection really uses

The safe importer logs each decision. For request f it allows scheme http on port 80 with no user info, looks up attacker.example once (203.0.113.9), finds it public and connects to it. The 302 redirect to 169.254.169.254 goes through the same checks, and is refused: 169.254.169.254 is link-local.
Every other request is refused by its real address, however it is spelled: 2852039166 is 169.254.169.254, link-local; 127.1 is 127.0.0.1, loopback; intranet.attacker.example is 10.0.0.5, private. images.example@10.0.0.5 is refused for its user info.
dual.attacker.example answers two addresses, 93.184.216.34 and 10.0.0.5; one private answer is enough to refuse the name. The honest image still imports.

### 5. A second lookup can give a different answer

The attacker's DNS server answers rebind.attacker.example with 203.0.113.9 the first time and 10.0.0.5 after that (DNS rebinding).
An importer that checks the name and then lets its HTTP client look the name up again sees 203.0.113.9, public, and then connects to 10.0.0.5: it fetches internal admin panel: 3 customers. The check was right; the connection went elsewhere.
The safe importer looks the name up once, checks 203.0.113.9 and connects to 203.0.113.9. The attacker's own host has no admin page: the server answered 404, and nothing inside was reached.

## The verdict

Prefer uploads, or a fixed list of hosts. If you must fetch arbitrary addresses: resolve once, refuse non-public addresses, connect to the checked address,
allow only http and https on the usual ports, follow few redirects and check each, limit type and size, and add network egress rules as a second layer.

## How to recognise this in code you did not write

- A form field named `url`, `callback`, `webhook`, `image_url` or `feed`.
- Library calls such as `new URL(input).openStream()` or `HttpClient.send` on a request built from input.
- Requests to 169.254.169.254 in logs.
- A finding titled "Server-Side Request Forgery" with CWE-918.

## Where you have already met this

- Webhook URLs, avatar imports, link previews, PDF renderers and "fetch this feed" features.
- The cloud metadata address 169.254.169.254 in every list of SSRF payloads.
- A breach report that begins with a server that fetched a URL for a user.
- A finding titled "Server-Side Request Forgery" with CWE-918.
