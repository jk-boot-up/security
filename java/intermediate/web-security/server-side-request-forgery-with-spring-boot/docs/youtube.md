# YouTube sheet — Server-Side Request Forgery with Spring Boot

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

SSRF in Spring Boot: RestClient, Resolved Addresses, Redirects and DNS Rebinding

## Description

Server-side request forgery, SSRF, in Spring Boot 4, in Java. A staff form that imports a product image from a web address lets Spring's RestClient fetch anything the server can reach: the cloud metadata service, localhost, and the office admin page. Explained with an online store, like a courier who will collect a parcel from any address you write down, including the locked rooms inside the warehouse. We watch a blocklist of names fail against a decimal spelling, a short loopback address and a redirect, then fix it: look the name up once, judge every address it resolves to, connect to the address that was judged, judge every redirect again, and accept only a small image that arrives in time. We finish with the mistake that looks like a fix, checking a name and then looking it up again, and the cost: a second wall of network rules for outgoing traffic.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/server-side-request-forgery-with-spring-boot

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:31 The Words
02:21 What the Letters Stand For
03:21 What the Letters Stand For (continued)
03:57 The shop stands inside
04:54 From the form to the fetch
05:50 Act One — The shop's import form works
06:03 Act One — The shop's import form works
06:16 Three addresses that point inside
07:05 Act Two — The server fetches whatever it is told
07:24 One address, many spellings
08:15 Act Three — A blocklist of names is easy to walk around
08:25 Act Three — A blocklist of names is easy to walk around
08:37 Act Three — A blocklist of names is easy to walk around
09:05 Check every answer, connect to the one you checked
09:58 Act Four — Check the address the connection really uses
10:15 Act Four — Check the address the connection really uses
10:24 Act Four — Check the address the connection really uses
10:44 Four limits, four refusals
11:34 Act Five — Limits on what is fetched
11:43 Act Five — Limits on what is fetched
11:49 Two lookups, two answers
12:43 Act Six — A second lookup can give a different answer
13:04 Act Six — A second lookup can give a different answer
13:17 What It Costs
13:57 Where You Have Seen It
14:31 How To Use It
15:09 The Java Classes
15:22 ImportController — POST /import
15:37 NaiveImporter — the name is looked up for every hop
15:52 GuardedImporter — every answer must be public
16:08 GuardedImporter — connect, then judge the redirect again
16:20 Fetcher — the client is told not to follow
16:36 AddressPolicy — judged from the address
16:48 Thanks for Watching
```

## Tags

ssrf, spring boot, restclient, dns rebinding, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/server-side-request-forgery-with-spring-boot-explained.mp4` |
| Captions | `video/server-side-request-forgery-with-spring-boot-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Web Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (80)
- [ ] tags are at most 500 characters in total — ok (118)
- [ ] description is at most 5,000 characters — ok (864)
- [ ] chapters start at 00:00 and there are at least three
