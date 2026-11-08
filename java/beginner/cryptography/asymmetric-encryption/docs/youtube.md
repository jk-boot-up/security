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
00:53 The Scenario
01:12 The Words
01:36 What the Letters Stand For
02:12 What the Letters Stand For (continued)
02:46 RSA by hand, with tiny numbers
03:46 A key sent in the clear is a key Eve has
04:26 Act One — The problem: sharing a key
04:45 Real RSA-2048 with OAEP
05:27 Act Two — A key pair: the public key locks, the private key opens
05:49 Act Two — A key pair: the public key locks, the private key opens
06:04 Hybrid encryption: the envelope
06:47 Act Three — Hybrid encryption: RSA for the key, AES for the data
07:03 Key agreement: the same secret, never sent
07:57 Act Four — Key agreement: no key is sent at all
08:10 Act Four — Key agreement: no key is sent at all
08:35 The man in the middle
09:15 Act Five — The bill: whose key is it?
09:36 Where You Have Seen It
09:59 How To Use It
10:20 The Java Classes
10:29 AsymmetricDemo — the five acts
10:41 ByHand — keys, lock, open
10:59 RsaBox — lock with the public key
11:12 HybridBox — seal
11:30 Envelope — what is sent
11:37 KeyAgreementBox — the shared secret, and the key
11:53 Wire — send
11:58 Hex — helpers
12:04 Thanks for Watching
```

## Tags

asymmetric encryption, rsa, oaep, hybrid encryption, x25519, diffie-hellman, hkdf, public key cryptography, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/asymmetric-encryption-explained.mp4` |
| Captions | `video/asymmetric-encryption-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Cryptography Basics in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (183)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
