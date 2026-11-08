# YouTube sheet — Asymmetric Encryption with Bouncy Castle and OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Asymmetric Encryption with Bouncy Castle and OpenSSL (PEM, CMS)

## Description

Asymmetric encryption with Bouncy Castle and OpenSSL, in Java: the JDK can do the maths, but real systems exchange keys and encrypted files in standard formats. Explained with an online bank and its customer Ana, like a diplomat's sealed pouch that any embassy in the world knows how to open. We write keys as PEM text and protect a private key with a password, make a self-signed certificate, seal a statement in a standard CMS envelope, have the openssl command-line tool open it as a check that it really is a standard, and finish with the bill: the easy call picks the older RSA padding, the library is large, and registering it is global.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/asymmetric-encryption-with-bouncy-castle

Chapters:

```
00:00 Introduction
01:08 The Scenario
01:30 The Words
01:54 What the Letters Stand For
02:31 What the Letters Stand For (continued)
03:00 What the Letters Stand For (continued)
03:22 What the Letters Stand For (continued)
03:29 What PEM does to one key
04:05 Base64, step by step: three bytes, four letters
04:48 Act One — The public key as PEM text
05:05 Act One — The public key as PEM text
05:17 The same private key, before and after locking
05:50 Act Two — The private key file, locked with a password
06:03 A self-signed certificate: the inputs and the result
06:42 Act Three — A certificate for the public key
06:53 What goes into a CMS envelope, by size
07:41 Act Four — A standard envelope: CMS EnvelopedData
07:50 Opening the envelope by hand
08:39 Act Five — Opening it by hand, then with openssl
08:53 The careful call, the easy call, and the bill
09:25 Act Six — The bill: what Bouncy Castle costs
09:37 Where You Have Seen It
09:59 How To Use It
10:19 The Java Classes
10:24 BouncyCastleDemo — the six acts
10:37 Pem — writing PEM, and locking the private key
10:48 Base64Steps — three bytes, four groups
11:01 Certificates — build and sign
11:12 CmsEnvelope — seal: OAEP by name, AES-256 for the data
11:30 EnvelopeSteps — unlock the key, decrypt, remove the padding
11:47 OpenSsl — run openssl cms -decrypt
11:56 Providers — a global registration
12:05 Hex — helpers
12:10 Thanks for Watching
```

## Tags

bouncy castle, openssl, pem, cms, x509 certificate, rsa oaep, hybrid encryption, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/asymmetric-encryption-with-bouncy-castle-explained.mp4` |
| Captions | `video/asymmetric-encryption-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (63)
- [ ] tags are at most 500 characters in total — ok (157)
- [ ] description is at most 5,000 characters — ok (643)
- [ ] chapters start at 00:00 and there are at least three
