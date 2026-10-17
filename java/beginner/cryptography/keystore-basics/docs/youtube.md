# YouTube sheet — Keystores and Key Management Basics

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Keystores and Key Management Basics in Java (PKCS12, HKDF, Rotation)

## Description

Keystores and key management basics in Java: the best encryption in the world fails if the key is lying around. Explained with an online bank, like a bank that spends a fortune on a vault and leaves the key under the doormat. We find a key hard-coded in a compiled class, see that a key file is only as private as its permissions, keep keys in an encrypted PKCS12 keystore that shows neither the key nor its name, derive keys from passwords with PBKDF2 and from a master key with HKDF, rotate keys using key names inside the messages, and finish with the bill: the keystore needs a password too, and the file records weaker protection settings than you might expect.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/keystore-basics

Chapters:

```
00:00 Introduction
01:14 The Scenario
01:41 The Words
02:11 Act One — A key written in the source code
02:46 Act Two — A key in a file outside the program
03:22 Act Three — A PKCS12 keystore: an encrypted file of keys
04:23 Act Four — Making keys from other secrets
05:13 Act Five — Replacing a key, and the bill
06:11 Places for a Key, Best Last
06:47 Who Does What
07:22 Where You Have Seen It
07:55 How To Use It
08:29 Thanks for Watching
```

## Tags

keystore, pkcs12, key management, key rotation, hkdf, pbkdf2, secrets, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/keystore-basics-explained.mp4` |
| Captions | `video/keystore-basics-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (96)
- [ ] description is at most 5,000 characters — ok (666)
- [ ] chapters start at 00:00 and there are at least three
