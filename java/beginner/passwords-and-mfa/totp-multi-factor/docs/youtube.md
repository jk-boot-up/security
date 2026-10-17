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
01:33 The Scenario
02:05 How the Code Is Made
02:48 Act One — A password alone
03:09 Act Two — Enrolling a second factor
04:16 Act Three — Logging in with a code, and clocks that disagree
05:04 Act Four — The same code twice
05:52 Act Five — The bill: what a code cannot do
06:49 What a Code Does, and Does Not
07:25 Who Does What
08:02 Where You Have Seen It
08:37 How To Use It
09:04 Thanks for Watching
```

## Tags

totp, one-time password, two-factor authentication, 2fa, mfa, hotp, rfc 6238, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/totp-multi-factor-explained.mp4` |
| Captions | `video/totp-multi-factor-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (39)
- [ ] tags are at most 500 characters in total — ok (103)
- [ ] description is at most 5,000 characters — ok (650)
- [ ] chapters start at 00:00 and there are at least three
