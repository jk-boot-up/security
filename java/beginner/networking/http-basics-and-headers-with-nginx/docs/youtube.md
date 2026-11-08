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
01:15 The Scenario
01:34 The Words
02:00 What the Letters Stand For
03:04 nginx at the front door, writing the headers
04:08 What nginx sends, as it comes
05:09 Act One — nginx in front of the shop, as it comes
05:26 Four requests: a page and a 404, without and with always
06:21 Act Two — add_header, and the word always
06:33 Act Two — add_header, and the word always
06:54 One add_header in a location drops all the others
07:55 Act Three — The inheritance trap
08:20 Five requests against two limits and two rules
09:28 Act Four — Limits, and requests that are ambiguous
09:40 Act Four — Limits, and requests that are ambiguous
09:55 The application's headers, passed on and then hidden
10:56 Act Five — Hiding what the application gives away, and the bill
11:19 Three Traps in add_header
11:41 Where You Have Seen It
12:08 How To Use It
12:31 The Java Classes
12:45 NginxConfig — the five headers, each with always
13:04 Nginx — a pinned image on a free local port
13:21 Probe — requests a normal client would never send
13:37 HeaderAudit — a header is only worth its value
13:49 Thanks for Watching
```

## Tags

nginx, http security headers, add_header, hsts, csp, request smuggling, proxy_hide_header, docker, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/http-basics-and-headers-with-nginx-explained.mp4` |
| Captions | `video/http-basics-and-headers-with-nginx-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (174)
- [ ] description is at most 5,000 characters — ok (684)
- [ ] chapters start at 00:00 and there are at least three
