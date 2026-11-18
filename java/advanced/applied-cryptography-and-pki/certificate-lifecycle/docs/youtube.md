# YouTube sheet — Certificate Lifecycle

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Certificate Lifecycle: OCSP, CRL, Stapling, Short-Lived Certificates and Missed Renewals

## Description

Certificate lifecycle for a bank: a private root and intermediate with openssl, CRL, OCSP and stapling with hard fail and soft fail, short-lived certificates renewed by step-ca, the outage of a missed renewal, and an expiry monitor over a fleet.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/applied-cryptography-and-pki/certificate-lifecycle

Chapters:

```
00:00 Introduction
00:43 The Scenario
01:14 The Words
02:00 What the Letters Stand For
02:40 What the Letters Stand For (continued)
03:15 What the Letters Stand For (continued)
03:27 The life of one certificate
04:34 Seven steps from a certificate to the root
05:46 Act One — Issue and read
06:03 Act One — Issue and read
06:19 Act One — Issue and read
06:32 Act One — Issue and read
06:52 Who decides when the answer is missing
07:56 Act Two — Revocation and a dead responder
08:18 Act Two — Revocation and a dead responder
08:43 Act Two — Revocation and a dead responder
09:09 The answer, carried by the server
10:09 Act Three — Stapling
10:42 Act Three — Stapling
10:54 A 12-second certificate, renewed at 8
11:51 Act Four — Short lives and rotation
12:14 Act Four — Short lives and rotation
12:26 A missed renewal, and a monitor that warns in time
13:33 Act Five — The missed renewal
14:00 Act Five — The missed renewal
14:28 Act Five — The missed renewal
14:36 What It Costs
15:10 Who Does What
15:53 Where You Have Seen It
16:24 How To Use It
16:54 The Java Classes
17:08 ChainSteps — names, signatures, the right to sign, the dates
17:26 Chains — hard fail or soft fail
17:42 StapleClient — the answer from the handshake
17:54 Renewer — when a renewal is due
18:04 ExpiryWatch — when to alert
18:17 Fleet — renewed in time, or an outage
18:29 Thanks for Watching
```

## Tags

certificate lifecycle, ocsp, crl, ocsp stapling, step-ca, short-lived certificates, certificate expiry, pki, openssl, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/certificate-lifecycle-explained.mp4` |
| Captions | `video/certificate-lifecycle-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Applied Cryptography and PKI in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (88)
- [ ] tags are at most 500 characters in total — ok (164)
- [ ] description is at most 5,000 characters — ok (245)
- [ ] chapters start at 00:00 and there are at least three
