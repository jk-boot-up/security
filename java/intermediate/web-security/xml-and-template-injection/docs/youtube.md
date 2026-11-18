# YouTube sheet — XML External Entities and Template Injection

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

XXE and Template Injection in Java

## Description

XML external entities, XXE, and server-side template injection, SSTI, in Java. An XML parser and a template engine both run instructions they find in their input: an invoice upload can make the parser read a file from the server, and a customer's name pasted into an e-mail template can run as an expression and print the application's mail password. Explained with an online store, like a form whose small print says: attach a copy of whatever is in the manager's drawer, and a clerk who obeys it. We watch both attacks work, then refuse the DOCTYPE that invoices never need, switch to a logic-less template engine with named, escaped holes, and pass the customer's text in as data, never as part of the template. We finish with the cost: some XML really needs a DOCTYPE, and parser settings differ between libraries.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/xml-and-template-injection

Chapters:

```
00:00 Introduction
01:07 The Scenario
01:42 The Words
02:25 What the Letters Stand For
03:34 What the Letters Stand For (continued)
03:54 Two readers that obey their input
04:53 An honest invoice: five lines in, three fields out
05:49 Act One — An invoice upload
06:07 A name for a server file, and a name that multiplies
07:18 Act Two — The parser reads what the file tells it to
07:40 Act Two — The parser reads what the file tells it to
08:00 One setting stops both attacks; the rest are extra layers
09:01 Act Three — Refuse the DOCTYPE
09:13 Act Three — Refuse the DOCTYPE
09:30 The pasted name, one method call at a time
10:29 Act Four — A template engine that evaluates expressions
10:43 Act Four — A template engine that evaluates expressions
10:59 Act Four — A template engine that evaluates expressions
11:20 Two holes, no expressions, every value escaped
12:13 Act Five — A logic-less engine keeps data as data
12:34 Act Five — A logic-less engine keeps data as data
12:44 What It Costs
13:27 How To Use It
14:02 The Java Classes
14:20 NaiveInvoiceParser — the default factory
14:32 SafeInvoiceParser — refuse the DOCTYPE, then extra layers
14:52 SafeInvoiceParser — a size limit before parsing
14:59 ExpressionEngine — every template can reach the application
15:13 ExpressionEngine — any public method will do
15:24 XmlTemplateDemo — the name pasted into the template
15:34 LogicLessEngine — a flat list, and escaping
15:47 Thanks for Watching
```

## Tags

xxe, xml external entity, ssti, template injection, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/xml-and-template-injection-explained.mp4` |
| Captions | `video/xml-and-template-injection-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (34)
- [ ] tags are at most 500 characters in total — ok (137)
- [ ] description is at most 5,000 characters — ok (818)
- [ ] chapters start at 00:00 and there are at least three
