# YouTube sheet — Passkeys and WebAuthn

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Passkeys and WebAuthn in Java (Phishing-Resistant Sign-In) - OWASP A07

## Description

Passkeys and WebAuthn explained in Java with only the JDK. Explained with an online bank's passwordless sign-in, like a house key cut to fit one front door, that simply does not turn in any other lock. We register a passkey and see that the bank stores only a public key, sign in with a fresh single-use challenge, watch a look-alike site get nothing at all while a typed password is phished, require a fingerprint for a payment, catch a copied key with the signature counter, and finish with the bill: a lost phone, recovery codes, and a password that must be switched off before the account is really phishing-resistant.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/identity-and-access/passkeys

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:19 The Words
01:50 Act One — Registering a passkey: the bank keeps only a public key
02:38 Act Two — Signing in: a fresh challenge, signed once
03:29 Act Three — A look-alike site gets nothing it can use
04:23 Act Four — User verification for payments, and counters against copies
05:19 Act Five — The bill: recovery, lost devices and the move away from passwords
06:10 The Checks, In Order
06:38 Who Does What
07:06 Where You Have Seen It
07:30 How To Use It
07:57 Thanks for Watching
```

## Tags

passkeys, webauthn, fido2, passwordless, phishing resistant, authentication, nist 800-63b, java, application security

## Files

| What | File |
| --- | --- |
| Video | `video/passkeys-explained.mp4` |
| Captions | `video/passkeys-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (109)
- [ ] description is at most 5,000 characters — ok (622)
- [ ] chapters start at 00:00 and there are at least three
