# YouTube sheet — Digital Signatures (Ed25519)

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Digital Signatures (Ed25519) Explained in Java

## Description

Digital signatures in Java: how can a message prove who made it, so that anyone can check but only one party can sign? Explained with an online store's price list, like the signature of a notary that anyone can compare with a specimen on file but nobody else can write. We see how an HMAC tag can be forged by anyone who can check it, sign a price list with Ed25519 and check it with only the public key, tie a signature to its purpose so it cannot be reused for another, stop a genuine old list being replayed with a version and an expiry inside the signed bytes, and finish with the bill: a signature is only as good as the key you check it against, and a stolen private key signs anything.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/digital-signatures

Chapters:

```
00:00 Introduction
01:09 The Scenario
01:35 The Words
02:04 Act One — A tag made with a shared key
02:49 Act Two — A signature: made with the private key, checked with the public key
03:43 Act Three — What exactly is signed
04:37 Act Four — A good signature on an old message
05:25 Act Five — The bill: whose key is it, and what if it is stolen?
06:10 What To Put Inside, And What Is Left
06:32 Who Does What
07:02 Where You Have Seen It
07:39 How To Use It
08:10 Thanks for Watching
```

## Tags

digital signatures, ed25519, public key cryptography, signing, verification, replay attack, cryptography basics, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/digital-signatures-explained.mp4` |
| Captions | `video/digital-signatures-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (46)
- [ ] tags are at most 500 characters in total — ok (138)
- [ ] description is at most 5,000 characters — ok (692)
- [ ] chapters start at 00:00 and there are at least three
