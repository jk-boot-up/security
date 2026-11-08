# YouTube sheet — Digital Signatures with Bouncy Castle and OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Digital Signatures with Bouncy Castle and OpenSSL (CMS, ECDSA, Ed25519, JWS)

## Description

Digital signatures with Bouncy Castle and OpenSSL, in Java: the JDK can sign, but real systems exchange signed messages in standard formats. Explained with an online bank, like a notary's stamp that any office in the world knows how to check. We sign a statement in the standard CMS format, attached and detached, work out an ECDSA signature step by step with real numbers, see why its nonce must never repeat and how deterministic signing removes the risk, reproduce the published Ed25519 and Ed448 vectors, build a compact signed token by hand and see why the verifier must choose the algorithm, and finish with the bill: a valid signature does not say who owns the key, and the signing time is only the signer's word.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/digital-signatures-with-bouncy-castle

Chapters:

```
00:00 Introduction
00:55 The Scenario
01:12 The Words
01:39 What the Letters Stand For
02:31 What the Letters Stand For (continued)
02:56 Signing one statement, and checking it
03:37 Inside a CMS signed message
04:22 Act One — A standard signed message: CMS SignedData
04:39 Act One — A standard signed message: CMS SignedData
04:56 How the signature r, s is made
05:45 Act Two — How an ECDSA signature is made, step by step
05:57 Act Two — How an ECDSA signature is made, step by step
06:23 One k twice gives the key away
07:11 Act Three — The nonce that must never repeat
07:31 Act Three — The nonce that must never repeat
07:49 How a JWS token is built
08:37 Act Four — A compact signed token: JWS
08:51 Act Four — A compact signed token: JWS
09:09 PEM armor, and what a signature does not say
09:49 Act Five — Text armor, and the bill
10:09 Where You Have Seen It
10:34 How To Use It
10:56 The Java Classes
11:15 Cms — sign: attributes, certificate, content
11:35 Cms — verify: every signer, one certificate
11:49 SignatureSteps — the four steps
12:07 Ecdsa — deterministic, random, and the reuse
12:22 Jws — sign, and a verifier that fixes the algorithm
12:38 Thanks for Watching
```

## Tags

digital signatures, bouncy castle, openssl, cms, ecdsa, ed25519, jws, rfc 6979, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/digital-signatures-with-bouncy-castle-explained.mp4` |
| Captions | `video/digital-signatures-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (155)
- [ ] description is at most 5,000 characters — ok (720)
- [ ] chapters start at 00:00 and there are at least three
