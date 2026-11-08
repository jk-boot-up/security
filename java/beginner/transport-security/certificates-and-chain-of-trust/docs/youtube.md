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
01:44 What the Letters Stand For
02:25 What the Letters Stand For (continued)
02:51 Trust flows down a chain
03:28 What a certificate is: a request, signed
04:16 Act One — A chain of three certificates
04:44 The browser's check, step by step
05:36 Act Two — The browser's check, step by step
05:54 Act Two — The browser's check, step by step
06:13 Three chains, and the step each one fails
06:57 Act Three — Three reasons to refuse
07:17 One changed letter, and a key that oversteps
07:54 Act Four — Changed certificates, and certificates that overstep
08:15 A stolen CA key, and revoking it
08:56 Act Five — Revocation, and the bill
09:25 What a Validator Checks
09:47 Where You Have Seen It
10:11 How To Use It
10:35 The Java Classes
10:44 CertsDemo — the five acts
10:57 CertBuilder — the signed part, then the signature
11:12 ChainSteps — each certificate against the next
11:29 ChainValidator — trusted roots, a date, a revocation list
11:44 HostName — a wildcard is one label
11:54 Tamper — change one byte inside the signed part
12:07 Thanks for Watching
```

## Tags

certificates, x509, chain of trust, pki, certificate authority, revocation, pkix, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/certificates-and-chain-of-trust-explained.mp4` |
| Captions | `video/certificates-and-chain-of-trust-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (69)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (665)
- [ ] chapters start at 00:00 and there are at least three
