# YouTube sheet — Hardware-Backed Keys

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Hardware-Backed Keys: PKCS#11, SoftHSM and What a Token Does Not Protect

## Description

Hardware-backed keys for a bank: a payment-signing key generated on a SoftHSM2 token, signing from Java with PKCS#11, five failed attempts to export it, PINs, a key in a file for contrast, key wrapping, and what a compromised host can still do.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/advanced/applied-cryptography-and-pki/hardware-backed-keys

Chapters:

```
00:00 Introduction
00:49 The Scenario
01:17 The Words
02:00 What the Letters Stand For
03:06 What the Letters Stand For (continued)
04:15 What the Letters Stand For (continued)
04:30 A file goes in, a signature comes out
05:28 What the bank asks for, and what the token made
06:27 Act One — The key ceremony
06:45 Act One — The key ceremony
07:03 Five ways to get the key out, five refusals
07:56 Act Two — Five ways out, and signing
08:13 Act Two — Five ways out, and signing
08:33 A token that never locks, and two kinds of copy
09:35 Act Three — The PIN, a key in a file, and a copied token
10:02 Act Three — The PIN, a key in a file, and a copied token
10:23 Fourteen requests, and a log that cannot be edited
11:28 Act Four — Rate, approvers and audit
11:49 Act Four — Rate, approvers and audit
12:06 An attacker already on the host
13:05 Act Five — A compromised host
13:34 A key wrapped for backup, and who else can unwrap it
14:38 Act Six — A key that can be backed up, and the limits
15:03 Act Six — A key that can be backed up, and the limits
15:26 What It Costs
16:05 Where You Have Seen It
16:37 How To Use It
17:11 The Java Classes
17:26 Raw — the template of the key ceremony
17:39 Raw — C_WrapKey: encrypt a key, or refuse
17:54 TokenAccess — login is loading the KeyStore with the PIN
18:11 PinGuard — stop asking after three failures
18:25 SigningService — approvers first, then the rate
18:38 AuditLog — each hash includes the one before
18:56 Malware — no PIN: use the login that is already there
19:12 Thanks for Watching
```

## Tags

hsm, pkcs11, softhsm, sunpkcs11, key management, code signing, payment signing, key wrapping, non-extractable keys, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/hardware-backed-keys-explained.mp4` |
| Captions | `video/hardware-backed-keys-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Applied Cryptography and PKI in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (72)
- [ ] tags are at most 500 characters in total — ok (162)
- [ ] description is at most 5,000 characters — ok (244)
- [ ] chapters start at 00:00 and there are at least three
