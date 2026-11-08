# YouTube sheet — Symmetric Encryption with Google Tink

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Symmetric Encryption with Google Tink (AEAD, Keysets)

## Description

Symmetric encryption with Google Tink, in Java: a library designed so that the easy way is the safe way. Explained with an online bank's account statements, like a lock box maker who picks the lock, the combination and the tamper seal for you, and offers no unsafe models. We seal a statement line with one call and watch Tink pick a fresh nonce each time, see a flipped bit and a moved message refused, rotate the key without losing old data, wrap each message's own data key with a master key, which is envelope encryption, and finish with the bill: the format is Tink's own, it adds a library, and the master key still has to live somewhere.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/cryptography/symmetric-encryption-with-tink

Chapters:

```
00:00 Introduction
00:49 The Scenario
01:08 The Words
01:44 What the Letters Stand For
02:37 What Tink does to one line
03:31 One call, sealed twice
04:30 Act One — One call, and no nonce to look after
04:46 Inside one Tink message
05:55 Act Two — Inside one Tink message, worked out by hand
06:09 Opening: the tag is checked first
07:01 Act Three — Any change is refused
07:08 Rotating a key in the keyset
08:02 Act Four — Replacing the key without losing the data
08:17 Envelope encryption: a key per message
09:22 Act Five — Keys that stay at home: envelope encryption
09:39 Act Five — Keys that stay at home: envelope encryption
10:01 The bill
10:50 Act Six — The bill: what Tink decides, and what it does not
11:06 How To Use It
11:35 The Java Classes
11:41 TinkDemo — the six acts
11:48 Bank — one primitive, two calls
12:08 DemoKeysets — a keyset with a known key and id
12:20 TinkLayout — prefix + nonce + AES-GCM
12:48 EnvelopeAead — encrypt: wrap, then seal
13:07 CountingAead — one call per message
13:15 Thanks for Watching
```

## Tags

tink, google tink, aead, aes-gcm, keyset, key rotation, encryption library, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/symmetric-encryption-with-tink-explained.mp4` |
| Captions | `video/symmetric-encryption-with-tink-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (53)
- [ ] tags are at most 500 characters in total — ok (152)
- [ ] description is at most 5,000 characters — ok (644)
- [ ] chapters start at 00:00 and there are at least three
