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
00:58 The Scenario
01:28 Act One — Importing an image
01:40 Act Two — The server fetches whatever it is told
02:03 Act Three — A blocklist of names is easy to walk around
02:31 Act Four — Check the address the connection really uses
02:58 Act Five — Why checking and then connecting is not enough
03:24 What It Costs
04:07 Who Does What
04:45 How To Use It
05:26 Thanks for Watching
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
