# Path Traversal, File Upload and Open Redirects, Explained

## The idea in one sentence

A file name or link from a customer is untrusted: check its shape, resolve it and compare paths as paths, let the server name and classify uploads and keep them out of the web root, and redirect only to paths on your own site.

## The 6 acts

### 1. Downloading an invoice

The download takes a name and reads that file from the invoices folder. invoice-1001.txt returns "Invoice 1001: Ana, 42.50".

### 2. A name with .. leaves the folder

The name is joined to the folder's path and nothing else is checked, so `..` climbs out: `../secrets/db.txt` returns DB_PASSWORD=demo-not-real and `../config/app.properties` returns the settings.
The customer never needed a vulnerability in the database, only a longer name.

### 3. A text check on the path is not a path check

The second version resolves the path, normalizes it and checks that the text starts with the invoices folder. `../secrets/db.txt` is refused. But `/…/invoices-private/payroll.txt`
also starts with `/…/invoices`, so the payroll file is read. Comparing paths as text treats a folder's name as a prefix; `Path.startsWith` compares whole elements.

### 4. Names, paths and links, all checked

The fixed version first decodes the name exactly once, then requires `invoice-NNNN.txt`. Then it resolves against the real invoices folder, normalizes and compares as a path. Finally it follows symbolic links
and checks the real file: a link named invoice-5555.txt that points at the secrets file passes the first checks and is refused with "link leads outside the invoice folder", while both older versions read it.
The encoded `%2e%2e%2f` is decoded and refused as not an invoice name.

### 5. Uploads

The naive upload saves under the name the customer gave, in the web root. A file named `page.html` with a script in it is served as text/html, inline: a page on the store's own site with the customer's script.
A name of `../../config/app.properties` replaces the settings, which now read admin=mallory. The fixed version limits the size, decides the type from the first bytes (PNG or JPEG only), makes the name itself
(photo-0001.png), ignores the customer's name entirely, stores the file outside the web root, and serves it as image/png with a download header and nosniff. The script renamed cat.png is refused for what it contains,
and a `../..` name is just ignored: the settings still read admin=ana.

### 6. Where to send the customer next

After sign-in or upload, the store sends the customer to the `return to` address. The naive version sends them wherever it says: a look-alike site at https://evil.example/login, `//evil.example` (a browser reads it
as another site), `/\evil.example` (browsers treat the backslash as a slash) or `javascript:`. The fixed version allows only a path that starts with a single slash, has no backslash or control characters, and has no scheme
or authority; everything else becomes /account. If an absolute address must be allowed, its host is compared exactly: shop.example.evil.example and shop.example@evil.example are refused.

## The verdict

Prefer ids to names, and mapping tables to paths. If you must use names: allow-list the shape, decode once, resolve against the real base folder, compare with `Path.startsWith`, and check the real path after links.
For uploads: size limit, magic-byte type, server-made name, storage outside the web root, `Content-Disposition: attachment` and `X-Content-Type-Options: nosniff`. For redirects: relative paths only, or an exact allow-list.

## How to recognise this in code you did not write

- `new File(base, name)`, `Paths.get(base, name)` or `base.resolve(name)` with request data.
- `getOriginalFilename()` used to build a path.
- A parameter named `next`, `url`, `redirect` or `returnTo` passed to a redirect.
- Findings with CWE-22, CWE-434 or CWE-601.

## Where you have already met this

- `../../etc/passwd` in every list of attack strings.
- `new File(base, userInput)` and `Paths.get(base, name)` in older code.
- A `next` or `returnUrl` parameter on every login page.
- Findings titled "Path Traversal" (CWE-22), "Unrestricted Upload" (CWE-434) and "Open Redirect" (CWE-601).
