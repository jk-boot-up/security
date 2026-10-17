# YouTube sheet — Cross-Site Request Forgery (CSRF) and Its Defences

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Cross-Site Request Forgery (CSRF) Defences in Java (Tokens, SameSite, Fetch Metadata)

## Description

Cross-site request forgery and its defences, in Java: a browser attaches a site's cookie to every request sent to that site, even one that another site started. Explained with an online bank and a transfer form. A page on another site makes Ana's logged-in browser send money she never meant to send; then a per-session token in every form stops it, SameSite cookies stop the cookie from riding along, Fetch Metadata and Origin checks show where the request came from, and the bill shows what each defence still misses: older browsers, same-site subdomains, forms that forgot the token, and why APIs with bearer tokens are exposed differently. Mapped to OWASP Top 10 A01 and CWE-352.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/web-security/cross-site-request-forgery

Chapters:

```
00:00 Introduction
01:04 The Scenario
01:21 The Words
01:53 Act One — A request Ana did not intend
02:28 Act Two — A token in every form
03:09 Act Three — SameSite cookies
03:47 Act Four — Where the request came from, and GET
04:30 Act Five — The bill
05:15 The Defences, Side by Side
05:45 Who Does What
06:14 Where You Have Seen It
06:41 How To Use It
07:13 Thanks for Watching
```

## Tags

csrf, cross-site request forgery, samesite, csrf token, fetch metadata, owasp top 10, web security, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/cross-site-request-forgery-explained.mp4` |
| Captions | `video/cross-site-request-forgery-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (85)
- [ ] tags are at most 500 characters in total — ok (118)
- [ ] description is at most 5,000 characters — ok (683)
- [ ] chapters start at 00:00 and there are at least three
