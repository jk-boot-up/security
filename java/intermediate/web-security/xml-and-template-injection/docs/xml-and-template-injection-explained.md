# XML External Entities and Template Injection, Explained

## The idea in one sentence

A parser or an engine that runs instructions found in its input must be told not to, so refuse the DOCTYPE, keep templates fixed and logic-less, and pass customer text in as data.

## The 5 acts

### 1. An invoice upload

A supplier uploads a small XML invoice with a customer, a total and a note. The JDK parser, as it comes, reads it and the shop stores the three fields.

### 2. The parser reads what the file tells it to

The uploaded file starts with a DOCTYPE that defines the entity xxe as the contents of a file on the server, and the note is `&xxe;`. The parser fetches the file and the shop stores it
as the note: demo-secret-from-the-server. A second file defines six levels of ten references each; the JDK's default expansion limit stops it, which is luck of the library and not a defence.

### 3. Refuse the DOCTYPE

An invoice has no use for a DOCTYPE, so the safe parser is told to refuse one. The honest invoice is read as before. The file with the external entity and the bomb are both
refused with the same message, before any entity is looked at. External DTDs and schemas, XInclude and entity expansion are switched off as well, and uploads over 10,000 characters are refused.

### 4. A template engine that evaluates expressions

The e-mail template engine evaluates `${customer.name}` by walking properties by reflection, and `app` is reachable from every template. The owner's template works. When the code builds the template
by pasting the customer's name into it, a name of `${app.config.smtpPassword}` prints the demo SMTP password, and `${customer.class.name}` shows the class that holds the data. The same name
passed to a fixed template as data is not evaluated again.

### 5. A logic-less engine keeps data as data

The fixed engine has `{{name}}` and `{{email}}` and nothing else: no expressions, no reflection, one pass. A name pasted into the template stays the text `${app.config.smtpPassword}`.
Markup in a name is escaped for HTML. The better habit is still to keep customer text out of the template: pass it as a value.

## The verdict

Parse untrusted XML with a DOCTYPE refusal and the external-access properties set to empty, test the configuration of every parser in the project, and prefer JSON when entities are not needed.
Write templates as fixed files with named, escaped holes. Never concatenate customer text into a template, and never give a template access to application objects.

## How to recognise this in code you did not write

- `DocumentBuilderFactory.newInstance()` followed straight by `newDocumentBuilder()`.
- `<!DOCTYPE` and `<!ENTITY` in an uploaded file.
- `String.format` or `+` used to build a template before rendering it.
- A finding titled "XML External Entity" with CWE-611 or "Template Injection" with CWE-1336.

## Where you have already met this

- `DocumentBuilderFactory`, `SAXParserFactory` and `XMLInputFactory` in every Java XML import.
- `<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>` in every list of attack strings.
- `${7*7}` and `{{7*7}}` as the first probe in a template injection test.
- A finding titled "XML External Entity" with CWE-611 or "Server-Side Template Injection" with CWE-1336.
