# YouTube sheet — CORS Done Right

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

CORS Done Right: Reflected Origins, null and Exact Allow-Lists

## Description

CORS in Java: what the same-origin policy does, how Access-Control-Allow-Origin is misconfigured, and an exact allow-list.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/api-security/cors

Chapters:

```
00:00 Introduction
00:50 The Scenario
01:27 The Words
02:09 What the Letters Stand For
03:14 The browser sends; the bank decides who reads
04:06 An origin is scheme, host and port
05:08 Act One — The bank's own page needs no CORS
05:33 Sending is not reading
06:38 Act Two — With no CORS headers, a page may send but not read
06:56 Act Two — With no CORS headers, a page may send but not read
07:19 Three policies, three origins, who reads the balance
08:25 Act Three — Three policies that look like fixes
08:37 Act Three — Three policies that look like fixes
08:49 Act Three — Three policies that look like fixes
09:07 One partner on the list, and what each page gets
10:04 Act Four — An exact allow-list names one partner
10:23 Act Four — An exact allow-list names one partner
10:41 Act Four — An exact allow-list names one partner
10:56 The same answer, with and without cookies
11:51 Act Five — A wildcard and cookies never combine
12:15 What It Costs
12:55 Where You Have Seen It
13:31 How To Use It
14:13 The Java Classes
14:26 Origin — three parts, all equal
14:43 CorsPolicy — two unsafe policies, and the fix
15:04 BankApi — the preflight and the log
15:22 Browser — simple requests, and the read check
15:42 Thanks for Watching
```

## Tags

cors, same-origin policy, access-control-allow-origin, owasp, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/cors-explained.mp4` |
| Captions | `video/cors-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: API Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (62)
- [ ] tags are at most 500 characters in total — ok (134)
- [ ] description is at most 5,000 characters — ok (122)
- [ ] chapters start at 00:00 and there are at least three
