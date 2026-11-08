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
02:02 What the Letters Stand For
02:34 What the Letters Stand For (continued)
02:42 Rules that travel inside certificates
03:24 Asked for, and read back
04:08 Act One — A chain built with Bouncy Castle
04:31 The name rule, by hand and by the JDK
05:10 Act Two — A limit the authority carries in its own certificate
05:34 A signed list of withdrawn certificates
06:11 Act Three — A revocation list signed by the issuer
06:36 A signing request: the private key stays home
07:18 Act Four — Asking for a certificate without handing over the private key
07:39 A stolen CA key, and the root's list
08:13 Act Five — A stolen authority key, and the bill
08:38 Plain Project, Then Library, Then Another Tool
09:02 Where You Have Seen It
09:29 How To Use It
09:51 The Java Classes
10:00 CertsBcDemo — the five acts
10:12 CertFactory — the rules, as named extensions
10:31 NameRule — the name, or ending in a dot and the name
10:45 Crls — serials, reasons, and the issuer's signature
10:58 SigningRequest — proof of possession, then the policy
11:14 Thanks for Watching
```

## Tags

certificates, chain of trust, bouncy castle, openssl, x509, crl, csr, name constraints, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/certificates-and-chain-of-trust-with-bouncy-castle-explained.mp4` |
| Captions | `video/certificates-and-chain-of-trust-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (91)
- [ ] tags are at most 500 characters in total — ok (163)
- [ ] description is at most 5,000 characters — ok (682)
- [ ] chapters start at 00:00 and there are at least three
