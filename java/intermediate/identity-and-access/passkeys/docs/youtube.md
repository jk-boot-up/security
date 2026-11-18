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
01:02 The Scenario
01:31 The Words
02:02 What the Letters Stand For
02:56 What the Letters Stand For (continued)
03:31 One key, one site
04:23 Registering: what goes out, what comes back, who keeps what
05:23 Act One — Registering a passkey: the bank keeps only a public key
05:48 Act One — Registering a passkey: the bank keeps only a public key
06:14 How the signature is made, byte by byte
07:36 Act Two — Signing in: a fresh challenge, signed once
07:48 Act Two — Signing in: a fresh challenge, signed once
08:11 Act Two — Signing in: a fresh challenge, signed once
08:32 The site rule and the signed origin, worked out
09:34 Act Three — A look-alike site gets nothing it can use
09:52 Act Three — A look-alike site gets nothing it can use
10:17 The flags byte, and the counter that catches a copy
11:26 Act Four — User verification for payments, and counters against copies
11:55 Act Four — User verification for payments, and counters against copies
12:12 Every way in, and the weakest one still switched on
13:08 Act Five — The bill: recovery, lost devices and the move away from passwords
13:30 Act Five — The bill: recovery, lost devices and the move away from passwords
13:51 The Checks, In Order
14:22 Where You Have Seen It
14:46 How To Use It
15:13 The Java Classes
15:29 RelyingParty — the checks, in order
15:45 RelyingParty — signature, then counter
15:57 Browser — a page may only ask for its own site
16:09 Authenticator — the sixty-nine signed bytes
16:23 AuthenticatorData — hash, flags, counter
16:34 ClientData — type, challenge, origin
16:46 FixedRandom — a fixed seed
17:00 Thanks for Watching
```

## Tags

passkeys, webauthn, fido2, passwordless, phishing resistant, authentication, nist 800-63b, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/passkeys-explained.mp4` |
| Captions | `video/passkeys-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Identity and Access in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (160)
- [ ] description is at most 5,000 characters — ok (622)
- [ ] chapters start at 00:00 and there are at least three
