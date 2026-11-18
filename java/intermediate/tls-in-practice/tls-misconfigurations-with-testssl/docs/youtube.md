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
01:04 The Scenario
01:40 The Words
02:25 What the Letters Stand For
03:33 What the Letters Stand For (continued)
04:39 What the Letters Stand For (continued)
04:51 One finding, from the tool to the gate
05:53 Eleven rules over one report, and one exception
06:57 Act One — A server that follows the guidance
07:28 Act One — A server that follows the guidance
07:54 Act One — A server that follows the guidance
08:06 Act One — A server that follows the guidance
08:22 The guided lines and the copied ones
09:31 Act Two — A configuration copied from the first search result
09:58 Act Two — A configuration copied from the first search result
10:21 A gate on severity, and the shop's gate
11:22 Act Three — A server that still speaks what old clients speak
11:52 Act Three — A server that still speaks what old clients speak
12:06 Act Three — A server that still speaks what old clients speak
12:23 The expiry and the name, worked out
13:30 Act Four — Certificates that clients will refuse
13:51 Act Four — Certificates that clients will refuse
14:13 The same server, by name and by address
15:04 Act Five — The bill
15:18 Act Five — The bill
15:36 What It Costs
16:08 Where You Have Seen It
16:38 How To Use It
17:05 The Java Classes
17:20 TestSsl — four checks, by name, at the loopback address
17:38 Report — read by id
17:56 Policy — a rule reads one testssl id
18:14 Policy — an exception carries a reason
18:24 WorkedOut — the name asked for, against the certificate's names
18:44 Nginx and LegacyServer — what the JDK must be told
19:03 Thanks for Watching
```

## Tags

testssl, testssl.sh, tls, ssl, cipher suites, hsts, tls audit, owasp top 10, a02, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-misconfigurations-with-testssl-explained.mp4` |
| Captions | `video/tls-misconfigurations-with-testssl-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (97)
- [ ] tags are at most 500 characters in total — ok (128)
- [ ] description is at most 5,000 characters — ok (498)
- [ ] chapters start at 00:00 and there are at least three
