# YouTube sheet — Server-Side Request Forgery (SSRF) and Its Defences

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Server-Side Request Forgery (SSRF) Defences in Java

## Description

Server-side request forgery, SSRF, and its defences, in Java. A feature that makes the server fetch a web address for a customer, like importing a product image, lets the customer reach whatever the server can reach: the cloud metadata service with its credentials, the server's own settings, and internal admin pages. Explained with an online store, like a courier who will collect a parcel from any address you write down, including the locked rooms inside the warehouse. We watch a blocklist of names fail against other spellings of the same address, a name that points inside, and a redirect, then fix it by checking the address the connection really uses, connecting to exactly that address, and checking every redirect again. We finish with the cost: an allow-list needs upkeep, and network rules for outgoing traffic still matter.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/server-side-request-forgery

Chapters:

```
00:00 Introduction
01:00 The Scenario
01:36 The Words
02:35 What the Letters Stand For
03:36 What the Letters Stand For (continued)
04:09 The server stands inside
05:06 One address, one lookup, one request
05:52 Act One — An honest import works
06:15 Three addresses that point inside
07:07 Act Two — The server fetches whatever it is told
07:23 One address, many spellings
08:26 Act Three — A blocklist of names is easy to walk around
08:37 Act Three — A blocklist of names is easy to walk around
08:47 Act Three — A blocklist of names is easy to walk around
09:14 Act Three — A blocklist of names is easy to walk around
09:36 Check every answer, connect to the one you checked
10:30 Act Four — Check the address the connection really uses
10:47 Act Four — Check the address the connection really uses
10:57 Act Four — Check the address the connection really uses
11:16 Two lookups, two answers
12:15 Act Five — A second lookup can give a different answer
12:36 Act Five — A second lookup can give a different answer
12:49 What It Costs
13:33 Where You Have Seen It
14:12 How To Use It
14:54 The Java Classes
15:07 NaiveImporter — look up, connect, follow
15:23 BlocklistImporter — the text of the host
15:38 Spelling — one number, four parts
15:55 SafeImporter — every answer must be public
16:11 SafeImporter — every redirect is checked again
16:21 AddressPolicy — inside kinds
16:37 Thanks for Watching
```

## Tags

ssrf, server-side request forgery, dns rebinding, owasp top 10, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/server-side-request-forgery-explained.mp4` |
| Captions | `video/server-side-request-forgery-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (51)
- [ ] tags are at most 500 characters in total — ok (136)
- [ ] description is at most 5,000 characters — ok (837)
- [ ] chapters start at 00:00 and there are at least three
