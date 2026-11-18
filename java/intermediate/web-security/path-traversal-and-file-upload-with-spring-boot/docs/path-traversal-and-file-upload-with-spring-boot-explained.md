# Path Traversal and File Upload with Spring Boot, Explained

## The idea in one sentence

A file name, a path and a return address from a request are the customer's words, so the shop makes its own names, checks where a path really leads, decides content from the bytes and only ever redirects to its own paths.

## The 8 acts

### 1. An honest name downloads one invoice

The demo makes the store's folders in a temporary folder, printed as .../, and starts the real Spring Boot shop three times over it, as the naive, the prefix and the fixed version.
The form sends GET /invoices?name=invoice-1001.txt. Spring decodes the parameter and hands the controller invoice-1001.txt; the naive version joins it to the folder, .../invoices/invoice-1001.txt, and answers 200 Invoice 1001: Ana, 42.50.

### 2. A name with .. climbs out of the folder

The browser encodes the slashes, so the request is GET /invoices?name=..%2Fsecrets%2Fdb.txt. Spring decodes the parameter once and the controller gets ../secrets/db.txt, exactly as typed.
Joined to the folder, the path is .../invoices/../secrets/db.txt; the disk reads .. as the folder above and opens .../secrets/db.txt, so the shop answers 200 DB_PASSWORD=demo-not-real. The same with ../config/app.properties: 200 admin=ana.

### 3. A text check is not a path check

The second version resolves and normalizes the path, then checks that its text starts with the text of the folder, ".../invoices".
../secrets/db.txt becomes .../secrets/db.txt: no as text and no as a path, so the shop answers 400 outside the invoice folder.
../invoices-private/payroll.txt becomes .../invoices-private/payroll.txt. As text it starts with .../invoices, so the shop answers 200 payroll: demo salaries. Compared as a path, element by element, invoices-private is not invoices, so Path.startsWith says no.

### 4. The fixed version checks in four layers

The fixed shop checks in four layers, in order, and the first that fails refuses: decode once (Spring decodes a request parameter once; SafeDownloads never decodes again), the shape (invoice-, four digits, .txt), the path (remove .., compare with Path.startsWith), and the real path (follow links, compare again).
GET /invoices?name=invoice-1001.txt gets 200. name=..%2Fsecrets%2Fdb.txt and name=%2e%2e%2fsecrets%2fdb.txt both reach the controller as ../secrets/db.txt and get 400 not an invoice name. The double-encoded name=%252e%252e%252fsecrets%252fdb.txt reaches it as %2e%2e%2fsecrets%2fdb.txt; it is not decoded a second time, so it too gets 400 not an invoice name. invoice-9999.txt gets 400 no such file.
A symbolic link .../invoices/invoice-5555.txt -> .../secrets/db.txt has a good shape and a path inside the folder. The naive and prefix shops answer 200 with the password; the fixed shop follows the link to .../secrets/db.txt and answers 400 link leads outside the invoice folder.

### 5. Spring's static handler follows a link

The fixed shop also maps the invoices folder with Spring's own static resource handler, /static-invoices/**, as a project would with addResourceLocations.
GET /static-invoices/..%2fsecrets/db.txt gets 400 and GET /static-invoices/%2e%2e/secrets/db.txt gets 404: Spring refuses dot-dot in its encoded spellings. But GET /static-invoices/invoice-5555.txt gets 200 DB_PASSWORD=demo-not-real, because the handler follows the symbolic link out of the folder.

### 6. The naive upload trusts the customer's name

An upload is a multipart POST /uploads, and each part names its file: Content-Disposition: form-data; name="file"; filename="../../config/app.properties". MultipartFile.getOriginalFilename() returns that name exactly as sent.
The first bytes show what each file is: cat.png 89 50 4e 47 (.PNG), page.html 3c 68 74 6d (<htm), the settings file 61 64 6d 69 (admi).
The naive shop saves each in .../webroot/uploads/ under its own name: cat.png gets 200 /uploads/cat.png, page.html is served back as text/html, inline, a page on the shop's own site, and the third name climbs out: .../config/app.properties now reads admin=mallory.

### 7. The fixed upload decides for itself

The fixed shop never uses the customer's name. Its rules: Spring's spring.servlet.multipart.max-file-size=2KB and the code's own limit of 1 to 1000 bytes; the type from the first bytes, PNG (89 50 4e 47) or JPEG (ff d8 ff); a name photo-NNNN counted by the server; storage in .../uploads-private/, outside the web root.
cat.png gets 200 /uploads/photo-0001.png. page.html and the same script renamed cat.png get 400 not a PNG or JPEG image. ../../config/app.properties with PNG bytes gets 200 /uploads/photo-0002.png: its name was ignored. A 1500-byte photo gets 400 size not allowed from the code; a 5000-byte one gets 413 from Spring before the controller runs.
GET /uploads/photo-0001.png answers Content-Type: image/png, Content-Disposition: attachment and X-Content-Type-Options: nosniff, and the settings still read admin=ana.

### 8. Return links stay on our own site

After signing in, the shop answers GET /continue?returnTo=... with a 302 redirect. The naive controller puts the link in the Location header as it is: https://evil.example/login, //evil.example and /\evil.example lead to another site, and javascript:steal() is a script.
The fixed controller keeps /orders?page=2 and sends the rest to /account: https://evil.example/login and javascript:steal() do not start with /; //evil.example starts with //, another host; /\evil.example has a backslash, which browsers read as /.
If an absolute link must be allowed, its host must be exactly shop.example: shop.example.evil.example is another host, and in https://shop.example@evil.example/x the host is evil.example and shop.example is only a user name.

## The verdict

Use name patterns or server-made ids, compare real paths element by element, refuse links that leave the folder, decide the type from the bytes, serve uploads as downloads with nosniff, set multipart limits and keep a second limit in code, and redirect only to a path on the site.

## How to recognise this in code you did not write

- `getOriginalFilename()` passed to a file API.
- `startsWith` on the text of a path.
- `addResourceLocations("file:...")` in a Spring configuration.
- A `returnTo` or `next` parameter after a login.

## Where you have already met this

- `@RequestParam String file` passed to `Path.resolve`.
- `MultipartFile.getOriginalFilename()` used as a file name.
- `spring.servlet.multipart.max-file-size` in `application.properties`.
- `response.sendRedirect(request.getParameter("next"))` after a login.
