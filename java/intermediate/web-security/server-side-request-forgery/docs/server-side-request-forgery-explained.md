# Server-Side Request Forgery (SSRF) and Its Defences, Explained

## The idea in one sentence

When a server fetches an address for a customer, it must test the address the connection will really use, connect to exactly that address, and test every redirect again.

## The 5 acts

### 1. Importing an image

The seller pastes http://images.example/cat.png, the server fetches it and stores PNG-cat. This is the feature working as intended.

### 2. The server fetches whatever it is told

Without a check, the address of the metadata service returns the demo credentials, localhost returns the server's own environment (DB_PASSWORD=demo-not-real) and
10.0.0.5 returns the internal admin panel. The seller sees them because the import shows what it fetched. The customer's request never touched those addresses; the server's did.

### 3. A blocklist of names is easy to walk around

The blocklist refuses the metadata address, localhost and anything starting with 10. or 192.168. But 2852039166 is the same address written as one number, 127.1 is
loopback, ::ffff:a9fe:a9fe is the metadata address in IPv6 clothes, intranet.attacker.example is a name whose DNS answer is 10.0.0.5, and attacker.example redirects to the
metadata address after the check. The blocklist read the text; the connection used the address.

### 4. Check the address the connection really uses

The safe importer parses the address by hand, refuses other schemes, other ports and user info, resolves the host once, requires every answer to be a public address,
connects to the address it checked, and runs the same checks again on every redirect. Each spelling of an inside address is refused with the real address named in the log.
A rebinding name is harmless, because the importer never asks DNS twice: it fetches only the attacker's own picture.

### 5. Why checking and then connecting is not enough

A check-then-connect importer resolves rebind.attacker.example, sees a public address and approves. Then the HTTP client resolves it again; the attacker's DNS now answers
10.0.0.5 and the client fetches the internal admin panel. The check was right, and the connection went elsewhere. The fix is not a better check: it is connecting to the address that was checked.

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
