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
00:52 The Scenario
01:17 The Words
01:43 What the Letters Stand For
02:29 One price list, signed and checked
03:06 A shared key: whoever checks can also tag
03:39 Act One — A tag made with a shared key
03:57 Only the public key, and three checks
04:39 Act Two — A signature: the private key signs, the public key checks
05:03 Act Two — A signature: the private key signs, the public key checks
05:14 How the 64 bytes are made
06:17 Act Three — How the signature is made, step by step
06:38 Act Three — How the signature is made, step by step
06:54 What is signed: label, zero byte, list
07:37 Act Four — What exactly is signed
07:54 A genuine old list, replayed
08:37 Act Five — A good signature on an old message
08:51 Whose key is it?
09:32 Act Six — The bill: whose key is it, and what if it is stolen?
09:45 Where You Have Seen It
10:05 How To Use It
10:30 The Java Classes
10:39 Signing — sign and verify
10:52 Signing — the label, a zero byte, the message
11:01 PriceList — encode
11:12 Publisher — publish
11:25 Checkout — check
11:40 SignatureSteps — sign, by hand
11:55 HmacPublisher — tag
12:04 Ed25519Keys — keys
12:14 Thanks for Watching
```

## Tags

digital signatures, ed25519, public key cryptography, signing, verification, replay attack, cryptography basics, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/digital-signatures-explained.mp4` |
| Captions | `video/digital-signatures-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (46)
- [ ] tags are at most 500 characters in total — ok (189)
- [ ] description is at most 5,000 characters — ok (692)
- [ ] chapters start at 00:00 and there are at least three
