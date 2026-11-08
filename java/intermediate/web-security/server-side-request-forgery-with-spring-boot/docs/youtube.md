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
01:31 Act One — Importing an image from a web address
01:47 Act Two — The server fetches whatever it is told
02:09 Act Three — A blocklist of names is easy to walk around
02:36 Act Four — Check the address the connection really uses
03:00 Act Five — Limits, and why checking and then connecting is not enough
03:32 What It Costs
04:12 Who Does What
04:48 Where You Have Seen It
05:22 How To Use It
06:00 Thanks for Watching
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
