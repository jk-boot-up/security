# Path Traversal and File Upload with Spring Boot, Explained

## The idea in one sentence

A file name, a path and a return address from a request are the customer's words, so the shop makes its own names, checks where a path really leads, decides content from the bytes and only ever redirects to its own paths.

## The 6 acts

### 1. Downloading an invoice

The first version resolves the name from the query parameter against the invoice folder and returns the file. The honest name, invoice-1001.txt, returns Ana's invoice.

### 2. A name with dot-dot leaves the folder

Spring hands the controller the name exactly as typed. A name with dot-dot segments walks up out of the invoice folder, and the shop returns the demo database password from the secrets folder and the application's settings.

### 3. A text check on the path is not a path check

The second version resolves and normalises the path and then checks that its text starts with the invoice folder's text. The secrets folder is refused, but invoices-private starts with the same letters, so the payroll file is read.

### 4. Names, paths and links, all checked

The fixed shop accepts only names shaped like an invoice name, resolves the path and compares it element by element, and then checks that the real path, after symbolic links, is still inside the real folder. Dot-dot, an encoded spelling, the sibling folder and a link all fail, and a missing file says so.
Spring's own static handler for the folder refuses an encoded slash with 400 and a dot-dot segment with 404, but serves a link inside the folder that points at the secrets file: it does not follow the same rule.

### 5. Uploads

The naive upload keeps getOriginalFilename as sent: a script called page.html is served as text/html inline, and a name with dot-dot overwrites the configuration with admin=mallory. The fixed shop makes its own name, decides the type from the first bytes, and serves the file as a download of
the sniffed type with nosniff, so a script, even one renamed cat.png, is refused and a name with dot-dot is ignored. A photo of 1500 bytes is refused by the code, and one of 5000 bytes by Spring's multipart limit before the controller runs.

### 6. Where to send the customer next

After an upload the shop redirects to the return address in the request. The naive controller sends the customer to an outside address, a protocol-relative address, a backslash trick or a script address. The fixed controller accepts only a path on this site and sends everything else to the account page.
Where an absolute address must be allowed, the host is compared exactly, so shop.example.evil.example and shop.example@evil.example are both refused.

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
