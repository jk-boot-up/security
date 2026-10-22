# YouTube sheet — TLS Misconfigurations with testssl.sh

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TLS Misconfigurations with testssl.sh: Scanning Local Servers and Writing a Gate (OWASP A02, A04)

## Description

The real testssl.sh 3.2 against five local servers: a nginx configured to the guidance, a configuration copied from the first search result with obsolete suites, no HSTS or security headers and a version in the Server header, a server written with the JDK that speaks TLS 1.0 and 1.1 and has no forward secrecy, an expired certificate, a certificate for another name, and a scan by address that raises a false alarm. The findings become a policy of eleven rules with exceptions that carry a reason.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/tls-misconfigurations-with-testssl

Chapters:

```
00:00 Introduction
01:27 The Scenario
01:45 Act One — A server that follows the guidance
02:10 Act Two — A configuration copied from the first search result
02:31 Act Three — A server that still speaks what old clients speak
02:52 Act Four — Certificates that clients will refuse
03:07 Act Five — The bill
03:25 What It Costs
03:49 Who Does What
04:11 Where You Have Seen It
04:29 How To Use It
04:47 Thanks for Watching
```

## Tags

testssl, testssl.sh, tls, ssl, cipher suites, hsts, tls audit, owasp top 10, a02, java

## Files

| What | File |
| --- | --- |
| Video | `video/tls-misconfigurations-with-testssl-explained.mp4` |
| Captions | `video/tls-misconfigurations-with-testssl-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (77)
- [ ] description is at most 5,000 characters — ok (498)
- [ ] chapters start at 00:00 and there are at least three
