# YouTube sheet — Asymmetric Encryption (RSA and Key Agreement)

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Asymmetric Encryption Explained in Java (RSA, X25519, Hybrid)

## Description

Asymmetric encryption in Java: how can an online bank and a customer share a secret key when they have never met and an eavesdropper hears everything? Explained with a mailbox that anyone can drop a letter into but only the owner can open. We show why a key cannot be sent in the clear, lock a key with an RSA public key using OAEP and see why the same message gives different bytes each time, carry a ten-thousand-byte statement with hybrid encryption, agree a key with X25519 and HKDF without sending any key at all, and finish with the bill: nothing in the maths says whose public key it is, so a man in the middle can read everything.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/asymmetric-encryption

Chapters:

```
00:00 Introduction
01:16 The Scenario
01:41 The Words
02:14 Act One — The problem: sharing a key
02:49 Act Two — A key pair: lock with the public key, open with the private key
03:44 Act Three — Hybrid encryption: RSA for the key, AES for the data
04:34 Act Four — Key agreement: no key is sent at all
05:26 Act Five — The bill: whose key is it?
06:30 Two Routes to a Shared Key
06:59 Who Does What
07:29 Where You Have Seen It
08:06 How To Use It
08:38 Thanks for Watching
```

## Tags

asymmetric encryption, rsa, oaep, hybrid encryption, x25519, diffie-hellman, hkdf, public key cryptography, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/asymmetric-encryption-explained.mp4` |
| Captions | `video/asymmetric-encryption-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (132)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
