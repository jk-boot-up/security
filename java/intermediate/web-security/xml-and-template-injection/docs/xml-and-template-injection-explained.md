# XML External Entities and Template Injection, Explained

## The idea in one sentence

A parser or an engine that runs instructions found in its input must be told not to, so refuse the DOCTYPE, keep templates fixed and logic-less, and pass customer text in as data.

## The 5 acts

### 1. An invoice upload

The upload is a small XML file: an invoice element holding a customer, a total and a note. The note is written Thanks &amp; see you soon, because a bare & is not allowed in XML.
&amp; is a built-in entity: a name that the parser replaces by its value, here the character &.
The JDK's parser, as it comes, reads the file and the shop stores three fields: customer Ana, total 42.50, note Thanks & see you soon.

### 2. The parser reads what the file tells it to

The uploaded file starts with a DOCTYPE, the part of an XML file that may define entities. It defines xxe with the word SYSTEM and the address of a file on the server, and the note is &xxe;.
The parser opens that file and puts its text where &xxe; stands, so the shop stores the note demo-secret-from-the-server. In the demo the file is a temporary file the program writes and deletes.
A second file, the entity bomb, is 464 characters long. a0 is lol; a1 holds ten a0; each level holds ten of the level below, up to a6. a6 is 10 x 10 x 10 x 10 x 10 x 10 = 1,000,000 copies of lol, 3,000,000 characters.
The JDK's default entity expansion limit stops it. That is luck of the library, not a setting of ours.

### 3. Refuse the DOCTYPE

An invoice has no use for a DOCTYPE, so the safe parser refuses one: the feature disallow-doctype-decl is true. The settings are read back from the parser the code builds.
The other settings are extra layers in case somebody switches the first one off later: secure processing is on, no external DTDs or schemas may be fetched, XInclude is off and entity references are not expanded. Uploads over 10,000 characters are refused before parsing.
The same three files again: the honest invoice is accepted, Ana and 42.50. The external entity and the bomb are both refused with a DOCTYPE is not allowed in an invoice, before any entity is looked at. An invoice of 10,095 characters is refused as too large.

### 4. A template engine that evaluates expressions

The e-mail engine evaluates ${...} by walking properties by reflection, and two names are reachable from every template: customer and app. app.config.smtpPassword is the mail password, here the marker demo-smtp-password.
The owner's template Hello ${customer.name}, your order shipped. prints Hello Ana, your order shipped.
A customer signs up with the name ${app.config.smtpPassword}. The code builds the template by pasting the name in. The engine works it out step by step: app is the App object; .config calls config() and gets an AppConfig; .smtpPassword calls smtpPassword() and gets demo-smtp-password. The e-mail says Hello demo-smtp-password.
${customer.class.name} walks customer -> getClass() -> getName() and prints com.jk.explore.security.xmlinj.Customer: any public method is reachable.
Passed as data to the fixed template Hello ${customer.name}, the same name prints as text: the engine does not evaluate its own output again.

### 5. A logic-less engine keeps data as data

The logic-less engine fills {{name}} and {{email}} from a flat list of two values and does nothing else: no expressions, no reflection, one pass over the template.
The owner's template prints Hello Ana, your order shipped. The name pasted into the template stays the text ${app.config.smtpPassword}, and so does the name passed as data.
A name with markup, <script>x</script>, is escaped for HTML: & becomes &amp;, < becomes &lt;, > becomes &gt;, " becomes &quot; and ' becomes &#39;. The e-mail says Hello &lt;script&gt;x&lt;/script&gt;, which a mail reader shows as text.
The better habit is still to keep customer text out of the template and pass it as a value.

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
