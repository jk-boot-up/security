# YouTube sheet — TLS Attacks and Downgrades

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TLS Attacks and Downgrades: Weak Servers, testssl.sh, SCSV and What TLS 1.3 Removed

## Description

TLS attacks and downgrades on deliberately weak local servers: openssl and testssl.sh scored against planted weaknesses, the fallback dance and TLS_FALLBACK_SCSV, a recorded session opened with a leaked key, and what TLS 1.3 removed.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/applied-cryptography-and-pki/tls-attacks-and-downgrades

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:34 The Words
02:25 What the Letters Stand For
03:46 What the Letters Stand For (continued)
04:49 What the Letters Stand For (continued)
05:45 A downgrade on one connection
06:47 What each server accepts, one connection per cell
08:01 Act One — The lab: a weak server and two controls
08:18 Act One — The lab: a weak server and two controls
08:31 Act One — The lab: a weak server and two controls
08:51 Six planted weaknesses: how each tool found them
09:53 Act Two — Our probes and testssl.sh, scored against the truth
10:11 Act Two — Our probes and testssl.sh, scored against the truth
10:28 Three clients through the same broken network
11:37 Act Three — The downgrade dance
12:07 Act Three — The downgrade dance
12:26 Which recording a leaked key opens, step by step
13:44 Act Four — A recorded session, and the server key leaks later
14:03 Act Four — A recorded session, and the server key leaks later
14:26 What a client asked the TLS 1.3 only server, and the answer
15:34 Act Five — What TLS 1.3 removed
15:54 Act Five — What TLS 1.3 removed
16:21 What It Costs
17:00 Where You Have Seen It
17:38 How To Use It
18:15 The Java Classes
18:29 Weakness — six weaknesses, a label and a short name
18:41 Server — the server is a real openssl process
18:55 Tap — reset every hello above a version
19:09 TlsAttacksDemo — retry with less, with or without the signal
19:23 TlsRecords — the downgrade marker
19:36 Decryptor — from the encrypted secret to the client's key
19:55 TestSsl — a weakness with no finding of its own
20:09 Thanks for Watching
```

## Tags

tls, tls downgrade, testssl.sh, openssl, forward secrecy, tls 1.3, fallback scsv, static rsa, cipher suites, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-attacks-and-downgrades-explained.mp4` |
| Captions | `video/tls-attacks-and-downgrades-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Applied Cryptography and PKI in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (83)
- [ ] tags are at most 500 characters in total — ok (155)
- [ ] description is at most 5,000 characters — ok (233)
- [ ] chapters start at 00:00 and there are at least three
