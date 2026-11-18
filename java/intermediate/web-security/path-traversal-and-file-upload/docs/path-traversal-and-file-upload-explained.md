# Path Traversal, File Upload and Open Redirects, Explained

## The idea in one sentence

A file name or link from a customer is untrusted: check its shape, resolve it and compare paths as paths, let the server name and classify uploads and keep them out of the web root, and redirect only to paths on your own site.

## The 7 acts

### 1. An honest name downloads one invoice

The demo makes the store's folders in a temporary folder, printed as .../: invoices (invoice-1001.txt and invoice-1002.txt), invoices-private (payroll.txt), secrets (db.txt), config (app.properties), webroot/uploads and uploads-private.
The customer sends the name invoice-1001.txt. The first version joins it to the invoices folder, .../invoices/invoice-1001.txt, and reads that file: Invoice 1001: Ana, 42.50.

### 2. A name with .. climbs out of the folder

The name ../secrets/db.txt is joined to the folder as it is: .../invoices/../secrets/db.txt. The disk reads .. as the folder above, so it opens .../secrets/db.txt and returns DB_PASSWORD=demo-not-real.
The same with ../config/app.properties: the disk opens .../config/app.properties and returns admin=ana. The customer needed no flaw in the database, only a longer name.

### 3. A text check is not a path check

The second version resolves and normalizes the path, then checks that its text starts with the text of the folder, ".../invoices".
../secrets/db.txt becomes .../secrets/db.txt: as text it does not start with .../invoices, so it is refused with "outside the invoice folder".
../invoices-private/payroll.txt becomes .../invoices-private/payroll.txt. As text it does start with .../invoices, because the folder's name is a prefix of invoices-private, and the payroll is read.
Compared as a path, element by element, the element invoices-private is not invoices, so Path.startsWith says no.

### 4. The fixed version checks in four layers

The fixed version checks in four layers, in order, and the first that fails refuses: decode once (%2e%2e%2f becomes ../), the shape (invoice-, four digits, .txt), the path (remove .., compare with Path.startsWith), and the real path (follow links, compare again).
invoice-1001.txt passes all four and is read. ../secrets/db.txt, %2e%2e%2fsecrets%2fdb.txt (decoded once to ../secrets/db.txt) and ../invoices-private/payroll.txt fail the shape: "not an invoice name". invoice-9999.txt has a good shape but there is no such file.
A symbolic link .../invoices/invoice-5555.txt -> .../secrets/db.txt has a good shape and a path inside the folder. The naive and the prefix version both read the password through it; the fixed version follows the link, finds the real path .../secrets/db.txt, and refuses: "link leads outside the invoice folder".

### 5. The naive upload trusts the customer's name

The customer sends a name and bytes. The first bytes show what a file really is: cat.png starts 89 50 4e 47 (.PNG), page.html starts 3c 68 74 6d (<htm), and the third file starts 61 64 6d 69 (admi).
The naive upload saves each one in .../webroot/uploads/ under the name it came with. cat.png becomes /uploads/cat.png. page.html is served back as text/html, inline: a page with the customer's script, on the store's own site.
The name ../../config/app.properties climbs out of the web root, and .../config/app.properties now reads admin=mallory.

### 6. The fixed upload decides for itself

The fixed upload never uses the customer's name. Its rules: a size of 1 to 1000 bytes; the type from the first bytes, PNG (89 50 4e 47) or JPEG (ff d8 ff); a name photo-NNNN counted by the server; storage in .../uploads-private/, outside the web root.
cat.png (PNG bytes, 11 bytes) becomes /photos/photo-0001.png. page.html and the same script renamed cat.png both start 3c 68 74 6d and are refused: not a PNG or JPEG image. ../../config/app.properties with PNG bytes becomes /photos/photo-0002.png: its name was ignored. big.png, 5000 bytes, is refused: size not allowed.
The download of photo-0001.png is sent with Content-Type: image/png, Content-Disposition: attachment and X-Content-Type-Options: nosniff, and the settings still read admin=ana.

### 7. Return links stay on our own site

After sign-in, the store sends the customer to the returnTo link. The naive version puts the link in the Location header as it is: https://evil.example/login, //evil.example and /\evil.example lead to another site, and javascript:steal() runs a script.
The fixed version keeps /orders?page=2 and sends the rest to /account: https://evil.example/login and javascript:steal() do not start with /; //evil.example starts with //, another host; /\evil.example has a backslash, which browsers read as /.
If an absolute link must be allowed, its host must be exactly shop.example: https://shop.example/x is allowed; shop.example.evil.example is another host; in https://shop.example@evil.example/x the host is evil.example and shop.example is only a user name.

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
