# YouTube sheet — Post-Quantum Cryptography

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Post-Quantum Cryptography in the JDK: ML-KEM, ML-DSA and Hybrid Key Exchange

## Description

Post-quantum cryptography in the JDK for a bank: ML-KEM and ML-DSA, a hybrid X25519 plus ML-KEM key exchange with HKDF worked by hand, size tables, implicit rejection, harvest now decrypt later with Mosca's inequality, an inventory that orders the migration, and a Bouncy Castle cross-check.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/applied-cryptography-and-pki/post-quantum-cryptography

Chapters:

```
00:00 Introduction
00:55 The Scenario
01:26 The Words
02:13 What the Letters Stand For
03:22 What the Letters Stand For (continued)
04:15 What the Letters Stand For (continued)
04:49 Is data recorded today already at risk?
05:47 Bytes on the wire, old and new
06:49 Act One — What has to cross the network
07:13 Act One — What has to cross the network
07:31 One ML-KEM exchange, and a flipped bit
08:32 Act Two — ML-KEM in the JDK
08:48 Act Two — ML-KEM in the JDK
09:08 One signed statement, five checks
10:03 Act Three — ML-DSA in the JDK
10:20 Act Three — ML-DSA in the JDK
10:33 Two secrets, one key: the hybrid exchange
11:42 Act Four — A hybrid key exchange: X25519 and ML-KEM-768
12:02 Act Four — A hybrid key exchange: X25519 and ML-KEM-768
12:25 Which data is late, and which code moves first
13:29 Act Five — Planning the migration
13:44 Act Five — Planning the migration
13:58 Act Five — Planning the migration
14:15 What It Costs
14:57 Where You Have Seen It
15:32 How To Use It
16:04 The Java Classes
16:17 Sizes — one handshake's keys and signatures
16:28 PostQuantumDemo — encapsulate, then decapsulate
16:41 Signatures — the same API as RSA
16:54 Hybrid — both secrets in, one key out
17:04 HkdfSteps — expand: one HMAC block
17:18 Mosca — x plus y greater than z
17:30 Inventory — a file moves with its most urgent use
17:41 Thanks for Watching
```

## Tags

post-quantum, ml-kem, ml-dsa, hybrid key exchange, harvest now decrypt later, mosca inequality, fips 203, fips 204, crypto agility, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/post-quantum-cryptography-explained.mp4` |
| Captions | `video/post-quantum-cryptography-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Applied Cryptography and PKI in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (76)
- [ ] tags are at most 500 characters in total — ok (178)
- [ ] description is at most 5,000 characters — ok (291)
- [ ] chapters start at 00:00 and there are at least three
