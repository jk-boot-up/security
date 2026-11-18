# YouTube sheet — Mutual TLS Between Services

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Mutual TLS Between Services: Client Certificates, Identity and Rotation

## Description

Mutual TLS between a bank's services with step-ca and the JDK: client certificates, the certificate chain checked by hand, authorization by the identity in the SAN and not by the network, short-lived certificates renewed with the server reloading trust, and what a revoked certificate can still do.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/service-identity-and-zero-trust/mutual-tls-between-services

Chapters:

```
00:00 Introduction
00:51 The Scenario
01:19 The Words
01:56 What the Letters Stand For
02:44 What the Letters Stand For (continued)
03:26 A certificate is a badge
04:30 From a chain to an identity
05:30 Act One — The identity in a certificate
05:48 Act One — The identity in a certificate
06:07 Refused before any code runs
07:02 Act Two — Who gets through the handshake
07:15 Act Two — Who gets through the handshake
07:30 Each caller, each call
08:25 Act Three — Allowed by identity, not by the network
08:43 Act Three — Allowed by identity, not by the network
08:55 Eight seconds of life
09:56 Act Four — Rotation: a certificate that lives 8 seconds
10:10 Act Four — Rotation: a certificate that lives 8 seconds
10:35 Revoked, and still accepted
11:28 Act Five — A stolen certificate, and revocation
11:52 Act Five — A stolen certificate, and revocation
12:08 What It Costs
12:36 Who Does What
13:18 Where You Have Seen It
13:51 How To Use It
14:26 The Java Classes
14:41 Ledger — no certificate, no connection
14:56 Ledger — identity, revocation, then the rule
15:07 Identities — type 6 is a URI
15:22 ChainSteps — names and signatures, link by link
15:38 Tls — reload without a restart
15:49 Ca — issue, renew, revoke
16:05 Thanks for Watching
```

## Tags

mutual tls, mtls, client certificates, step-ca, zero trust, service identity, certificate rotation, revocation, private ca, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/mutual-tls-between-services-explained.mp4` |
| Captions | `video/mutual-tls-between-services-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Service Identity and Zero Trust in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (71)
- [ ] tags are at most 500 characters in total — ok (170)
- [ ] description is at most 5,000 characters — ok (298)
- [ ] chapters start at 00:00 and there are at least three
