# YouTube sheet — Asymmetric Encryption with Bouncy Castle and OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Asymmetric Encryption with Bouncy Castle and OpenSSL (PEM, CMS)

## Description

Asymmetric encryption with Bouncy Castle and OpenSSL, in Java: the JDK can do the maths, but real systems exchange keys and encrypted files in standard formats. Explained with an online bank and its customer Ana, like a diplomat's sealed pouch that any embassy in the world knows how to open. We write keys as PEM text and protect a private key with a password, make a self-signed certificate, seal a statement in a standard CMS envelope, have the openssl command-line tool open it as a check that it really is a standard, and finish with the bill: the easy call picks the older RSA padding, the library is large, and registering it changes the whole program.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/asymmetric-encryption-with-bouncy-castle

Chapters:

```
00:00 Introduction
01:17 The Scenario
01:48 Three Formats
02:22 Act One — Keys as PEM text
03:18 Act Two — A certificate for the public key
04:06 Act Three — A standard envelope: CMS EnvelopedData
04:44 Act Four — A different program opens it
05:32 Act Five — The bill: what Bouncy Castle costs
06:29 JDK, and Library
06:56 Who Does What
07:26 Where You Have Seen It
08:01 How To Use It
08:27 Thanks for Watching
```

## Tags

bouncy castle, openssl, pem, cms, x509 certificate, rsa oaep, hybrid encryption, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/asymmetric-encryption-with-bouncy-castle-explained.mp4` |
| Captions | `video/asymmetric-encryption-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (63)
- [ ] tags are at most 500 characters in total — ok (106)
- [ ] description is at most 5,000 characters — ok (659)
- [ ] chapters start at 00:00 and there are at least three
