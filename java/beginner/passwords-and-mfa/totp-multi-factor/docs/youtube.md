# YouTube sheet — One-Time Codes (TOTP)

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

One-Time Codes (TOTP) Explained in Java

## Description

One-time codes (TOTP) in Java: a second factor made from a secret that the phone and the bank share, and the current time. Explained with an online bank, like a safe-deposit box that needs your key and a bank clerk's key turned together, where the clerk's key changes every thirty seconds. We build the code from HMAC following RFC 4226 and RFC 6238, check it against the published test values, see how a window of one step forgives a clock that is a little out, stop a code being used twice, and finish with the bill: a code typed into a fake bank page still works, the secret also lives on the bank's server, and recovery codes are a second way in.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/totp-multi-factor

Chapters:

```
00:00 Introduction
00:57 The Scenario
01:26 The Words
01:56 What the Letters Stand For
02:46 What the Letters Stand For (continued)
03:17 Two sides, one code, no message
03:59 One factor: the password is all it takes
04:37 Act One — A password alone
04:50 The secret, written as letters for the app
05:44 Act Two — Enrolling: one secret, shared once
05:58 Act Two — Enrolling: one secret, shared once
06:15 How the six digits are made
07:25 Act Three — How the code is made, step by step
07:43 Act Three — How the code is made, step by step
08:00 A window of one step either side
08:55 Act Four — Logging in, and clocks that disagree
09:14 The same code twice
10:01 Act Five — The same code twice
10:13 Three things a code cannot do
11:11 Act Six — The bill: what a code cannot do
11:25 Act Six — The bill: what a code cannot do
11:35 Where You Have Seen It
12:01 How To Use It
12:27 The Java Classes
12:36 TotpDemo — the six acts
12:45 CodeSteps — the four steps
13:08 Hotp — HMAC, then a few bytes as digits
13:20 Totp — the counter is the time step
13:31 Base32 — 5 bits per letter
13:42 Authenticator — a clock that may be wrong
13:52 MfaServer — the window and the replay check
14:14 SecureSecrets — random, not the test key
14:29 Thanks for Watching
```

## Tags

totp, one-time password, two-factor authentication, 2fa, mfa, hotp, rfc 6238, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/totp-multi-factor-explained.mp4` |
| Captions | `video/totp-multi-factor-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (39)
- [ ] tags are at most 500 characters in total — ok (154)
- [ ] description is at most 5,000 characters — ok (650)
- [ ] chapters start at 00:00 and there are at least three
