# YouTube sheet — HTTP Basics and Security Headers with nginx

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

HTTP Security Headers with nginx (always, Inheritance Trap, Smuggling)

## Description

HTTP basics and security headers with nginx, in Java: the web server in front of the shop is the best place to set headers, and the easiest place to get them subtly wrong. Explained with an online store behind a real nginx container, like a doorman who hands every visitor the house rules, except in the side rooms where the rules were never written down again. We add the five security headers and score them with an auditor, see why the word always matters on error pages, fall into the add_header inheritance trap, watch nginx refuse an oversized request and the Content-Length with Transfer-Encoding smuggling pattern, and hide the technology the application behind it gives away.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/http-basics-and-headers-with-nginx

Chapters:

```
00:00 Introduction
01:09 The Scenario
01:27 The Words
01:54 Act One — nginx in front of the shop, as it comes
02:15 Act Two — add_header, and the word always
02:38 Act Three — The inheritance trap
03:10 Act Four — Limits, and requests that are ambiguous
03:46 Act Five — Hiding what the application gives away, and the bill
04:19 Three Traps in add_header
04:41 Who Does What
05:09 Where You Have Seen It
05:35 How To Use It
05:58 Thanks for Watching
```

## Tags

nginx, http security headers, add_header, hsts, csp, request smuggling, proxy_hide_header, docker, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/http-basics-and-headers-with-nginx-explained.mp4` |
| Captions | `video/http-basics-and-headers-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (123)
- [ ] description is at most 5,000 characters — ok (684)
- [ ] chapters start at 00:00 and there are at least three
