# YouTube sheet — Certificates and the Chain of Trust

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Certificates and the Chain of Trust in Java (X.509, PKIX, Revocation)

## Description

Certificates and the chain of trust in Java: how a browser knows that a public key really belongs to shop.example. Explained with an online store, like a passport signed by an office whose stamp is signed by a ministry you already trust. We build a root, an intermediate and a server certificate by writing their encoding byte by byte, validate the chain with the JDK, then watch it refuse an expired certificate, a certificate from an authority nobody trusts, a changed certificate, and a server certificate that tries to issue others. We check the host name as the separate step it is, and finish with the bill: revocation, and what a stolen authority key allows.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/certificates-and-chain-of-trust

Chapters:

```
00:00 Introduction
01:02 The Scenario
01:18 The Words
01:44 Act One — A chain of three certificates
02:16 Act Two — The browser's check
02:42 Act Three — Three reasons to refuse
03:14 Act Four — Changed certificates, and certificates that overstep
03:51 Act Five — Revocation, and the bill
04:37 What a Validator Checks
04:59 Who Does What
05:27 Where You Have Seen It
05:51 How To Use It
06:15 Thanks for Watching
```

## Tags

certificates, x509, chain of trust, pki, certificate authority, revocation, pkix, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/certificates-and-chain-of-trust-explained.mp4` |
| Captions | `video/certificates-and-chain-of-trust-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (69)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (665)
- [ ] chapters start at 00:00 and there are at least three
