# YouTube sheet — Certificates and Chain of Trust with Bouncy Castle and OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Certificates and Chain of Trust with Bouncy Castle and OpenSSL (CRL, CSR, Name Constraints)

## Description

Certificates and chain of trust with Bouncy Castle and OpenSSL, in Java: the JDK checks certificates but cannot make them. Explained with an online store, like a passport office that issues passports, limits what each office may issue, and publishes a list of lost ones. We build a root, an intermediate and a server certificate with Bouncy Castle, limit the intermediate to the store's own names, publish signed revocation lists, send a signing request so the private key never leaves the shop, have the openssl command-line tool verify the same chain, and finish with the bill: a stolen authority key breaks everything below it, and every extension is a place to be quietly wrong.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/certificates-and-chain-of-trust-with-bouncy-castle

Chapters:

```
00:00 Introduction
01:10 The Scenario
01:34 The Words
02:02 Act One — A chain built with Bouncy Castle
02:48 Act Two — A limit the authority carries in its own certificate
03:23 Act Three — A revocation list signed by the issuer
04:10 Act Four — Asking for a certificate without handing over the private key
04:55 Act Five — A stolen authority key, and the bill
05:49 Plain Project, Then Library, Then Another Tool
06:14 Who Does What
06:45 Where You Have Seen It
07:12 How To Use It
07:34 Thanks for Watching
```

## Tags

certificates, chain of trust, bouncy castle, openssl, x509, crl, csr, name constraints, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/certificates-and-chain-of-trust-with-bouncy-castle-explained.mp4` |
| Captions | `video/certificates-and-chain-of-trust-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (91)
- [ ] tags are at most 500 characters in total — ok (112)
- [ ] description is at most 5,000 characters — ok (682)
- [ ] chapters start at 00:00 and there are at least three
