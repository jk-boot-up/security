# YouTube sheet — HTTP Basics and Security Headers

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

HTTP Basics and Security Headers in Java (Cookies, Smuggling, Injection)

## Description

HTTP basics and security headers in Java: an HTTP message is text, and the sender writes every line. Explained with an online store. We parse and write a request byte by byte, refuse messages that two programs could read differently (the idea behind request smuggling), refuse header injection through a customer's search text, judge session cookies for HttpOnly, Secure and SameSite, and score a real response from a real local server against five security headers.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/http-basics-and-headers

Chapters:

```
00:00 Introduction
00:51 The Scenario
01:10 The Words
01:39 What the Letters Stand For
02:24 One request, as plain text
03:24 From text to fields, and back to text
04:15 Act One — An HTTP message is just text
04:48 One request, two ends: a request hidden after the body
05:53 Act Two — Messages that two programs could read differently
06:18 Act Two — Messages that two programs could read differently
07:00 A line break in the search text adds a header
08:00 Act Three — Header injection
08:19 Four cookies, checked one attribute at a time
09:22 Act Four — Cookies that carry a login
09:37 Act Four — Cookies that carry a login
09:49 Five headers, three responses, three scores
10:52 Act Five — Security headers on a real response
11:13 Act Five — Security headers on a real response
11:42 Where You Have Seen It
12:06 How To Use It
12:34 The Java Classes
12:45 HttpRequest — writing the request as text
13:06 HttpParser — the checks that stop smuggling
13:26 HttpResponse — no line break in a header value
13:42 Cookie — what is wrong for a login cookie
14:00 HeaderAudit — a header is only worth its value
14:18 LoopbackExchange — a real exchange over a socket
14:35 Thanks for Watching
```

## Tags

http, security headers, cookies, httponly, samesite, request smuggling, header injection, hsts, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/http-basics-and-headers-explained.mp4` |
| Captions | `video/http-basics-and-headers-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (171)
- [ ] description is at most 5,000 characters — ok (466)
- [ ] chapters start at 00:00 and there are at least three
