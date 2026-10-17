# YouTube sheet — Digital Signatures with Bouncy Castle and OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Digital Signatures with Bouncy Castle and OpenSSL (CMS, ECDSA, Ed25519, JWS)

## Description

Digital signatures with Bouncy Castle and OpenSSL, in Java: the JDK can sign, but real systems exchange signed messages in standard formats. Explained with an online bank, like a notary's stamp that any office in the world knows how to check. We sign a statement in the standard CMS format, attached and detached, see why ECDSA needs a nonce that never repeats and how deterministic signing removes the risk, reproduce the published Ed25519 and Ed448 vectors, build a compact signed token by hand and see why the verifier must choose the algorithm, and finish with the bill: a valid signature does not say who owns the key, and the signing time is only the signer's word.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/digital-signatures-with-bouncy-castle

Chapters:

```
00:00 Introduction
01:07 The Scenario
01:27 The Words
01:57 Act One — A standard signed message: CMS SignedData
02:33 Act Two — ECDSA: the nonce that must never repeat
03:09 Act Three — EdDSA: Ed25519 and Ed448
03:37 Act Four — A compact signed token: JWS
04:07 Act Five — Text armor, and the bill
04:42 What a Signature Does and Does Not Prove
05:05 Who Does What
05:46 Where You Have Seen It
06:18 How To Use It
06:41 Thanks for Watching
```

## Tags

digital signatures, bouncy castle, openssl, cms, ecdsa, ed25519, jws, rfc 6979, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/digital-signatures-with-bouncy-castle-explained.mp4` |
| Captions | `video/digital-signatures-with-bouncy-castle-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (104)
- [ ] description is at most 5,000 characters — ok (671)
- [ ] chapters start at 00:00 and there are at least three
