# YouTube sheet — Certificate Pinning and What It Costs

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Certificate Pinning in Java: Backup Pins, Rotation Outages and Why HPKP Died

## Description

Certificate pinning in Java for a mobile banking app, with real certificate chains: an interceptor whose root the device trusts, an SPKI pin that refuses it, the outage when the server renews with a new key (10,000 installs fail at once), pinning an issuing CA or a leaf with a backup key, a report-only mode, and why browsers dropped server-set pins (HPKP).

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/certificate-pinning

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:26 The Words
01:59 What the Letters Stand For
02:56 What the Letters Stand For (continued)
03:37 Trust the phone, or pin a key
04:31 Every root on the phone can vouch
05:36 Act One — An app that trusts the device
05:44 Act One — An app that trusts the device
05:56 How a pin is made
07:19 Act Two — An app that pins the server's key
07:30 Act Two — An app that pins the server's key
07:42 Act Two — An app that pins the server's key
08:02 The day the key changes
09:00 Act Three — The server renews with a new key
09:22 Pins that survive a renewal
10:19 Act Four — Pins chosen so that rotation does not break the app
10:27 Act Four — Pins chosen so that rotation does not break the app
10:42 One header, a year of lockout
11:57 Act Five — Why browsers stopped letting servers set pins
12:05 Act Five — Why browsers stopped letting servers set pins
12:26 What It Costs
13:02 Where You Have Seen It
13:36 How To Use It
14:14 The Java Classes
14:29 PinnedClient — validation first, then the pin
14:49 Spki — hash the key, then Base64
15:05 PinSteps — Base64 by hand
15:23 ChainWalk — which root signed it, which key is pinned
15:38 PinStore — remembered until max-age runs out
15:50 Thanks for Watching
```

## Tags

certificate pinning, hpkp, spki, tls, mobile security, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/certificate-pinning-explained.mp4` |
| Captions | `video/certificate-pinning-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (126)
- [ ] description is at most 5,000 characters — ok (358)
- [ ] chapters start at 00:00 and there are at least three
