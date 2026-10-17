# YouTube sheet — Symmetric Encryption with Google Tink

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Symmetric Encryption with Google Tink (AEAD, Keysets)

## Description

Symmetric encryption with Google Tink, in Java: a library designed so that the easy way is the safe way. Explained with an online bank's account statements, like a lock box maker who picks the lock, the combination and the tamper seal for you, and offers no unsafe models. We seal a statement line with one call and watch Tink pick a fresh nonce each time, see a flipped bit and a moved message refused, rotate the key without losing old data, store the keyset encrypted under a master key, and finish with the bill: the format is Tink's own, it adds a library, and the master key still has to live somewhere.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/symmetric-encryption-with-tink

Chapters:

```
00:00 Introduction
01:13 The Scenario
01:42 The Parts of Tink
02:10 Act One — One call, and no nonce to look after
02:59 Act Two — Any change is refused
03:33 Act Three — Replacing the key without losing the data
04:23 Act Four — Keys that stay at home: envelope encryption
05:33 Act Five — The bill: what Tink decides for you, and what it does not
06:17 Plain Version, Tink Version
06:52 Who Does What
07:19 Where You Have Seen It
07:52 How To Use It
08:22 Thanks for Watching
```

## Tags

tink, google tink, aead, aes-gcm, keyset, key rotation, encryption library, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/symmetric-encryption-with-tink-explained.mp4` |
| Captions | `video/symmetric-encryption-with-tink-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (53)
- [ ] tags are at most 500 characters in total — ok (101)
- [ ] description is at most 5,000 characters — ok (609)
- [ ] chapters start at 00:00 and there are at least three
