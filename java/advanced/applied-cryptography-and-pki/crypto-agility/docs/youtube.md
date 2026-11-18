# YouTube sheet — Crypto Agility

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Crypto Agility: Versioned Envelopes, Rotating Algorithms and Retiring Keys

## Description

Crypto agility for a bank: an inventory of algorithm names in source, a versioned and authenticated envelope, migration by read-repair and by batch, a second rotation, retiring a key and the backup that still needs it, and downgrade protection.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/applied-cryptography-and-pki/crypto-agility

Chapters:

```
00:00 Introduction
00:49 The Scenario
01:24 The Words
02:06 What the Letters Stand For
03:29 What the Letters Stand For (continued)
04:44 What the Letters Stand For (continued)
05:13 One record, sealed in three suites
06:25 Ten files, twenty names, and what the policy says
07:26 Act One — What the code asks for
07:40 Act One — What the code asks for
08:06 Act One — What the code asks for
08:22 Old and new records, and two readers
09:23 Act Two — A value that says how it was protected
09:52 Act Two — A value that says how it was protected
10:10 From key 1 to key 2, in two steps
11:11 Act Three — Moving the data
11:32 Act Three — Moving the data
11:45 No live record needs key 1, but a backup does
12:44 Act Four — The next rotation, and retiring a key
13:05 Act Four — The next rotation, and retiring a key
13:24 Three changed headers, three refusals
14:30 Act Five — Tampering and downgrade
14:56 What It Costs
15:31 Where You Have Seen It
16:03 How To Use It
16:36 The Java Classes
16:46 Inventory — a pattern for every getInstance call
17:03 Policy — a status for every name
17:17 Envelope — seal: the header, then the suite it names
17:39 Envelope — open: the policy first
17:53 KeyRing — derive a key from the master key
18:13 Store — read-repair
18:29 Layout — the parts follow from the version
18:44 Thanks for Watching
```

## Tags

crypto agility, key rotation, algorithm migration, versioned encryption, aes-gcm, chacha20-poly1305, cryptographic inventory, downgrade, post-quantum readiness, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/crypto-agility-explained.mp4` |
| Captions | `video/crypto-agility-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Applied Cryptography and PKI in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (74)
- [ ] tags are at most 500 characters in total — ok (207)
- [ ] description is at most 5,000 characters — ok (244)
- [ ] chapters start at 00:00 and there are at least three
