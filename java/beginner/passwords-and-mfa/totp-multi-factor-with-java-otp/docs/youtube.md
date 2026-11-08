# YouTube sheet — One-Time Codes with the java-otp Library

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

One-Time Codes (TOTP) with the java-otp Library

## Description

One-time codes with the java-otp library, in Java: a library can make the six-digit code for you and check it for one time step, but a bank still has to decide the rest. Explained with an online bank, like a clerk who buys a ready-made stamp but still has to decide who may use it and how often. We check the library's codes against the values published in RFC 4226 and RFC 6238, see that it validates only a single step so the window is written by the bank, try SHA-256 and SHA-512 and eight digits, build the otpauth link a QR code carries, and finish with the bill: the library keeps no replay memory, no secrets and no attempt limits.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/passwords-and-mfa/totp-multi-factor-with-java-otp

Chapters:

```
00:00 Introduction
01:05 The Scenario
01:27 The Words
01:56 What the Letters Stand For
02:47 What the Letters Stand For (continued)
03:11 The same secret, the same clock, the same code
03:48 One code per 30-second step
04:36 Act One — The library's codes, checked against the standard
04:43 How 287082 is made
05:55 Act Two — How the code at time 59 is made, step by step
06:05 One step, or a window
06:55 Act Three — What the library checks, and what the bank adds
07:05 Three hashes, eight digits
07:49 Act Four — Other hashes and longer codes
07:53 The text in the QR code
08:40 Act Five — Giving the secret to the phone
08:49 The same code, twice
09:31 Act Six — The bill: what the library does not do
09:39 Library, and Bank
10:06 Where You Have Seen It
10:27 How To Use It
10:51 The Java Classes
10:59 OtpLibraryDemo — the six acts
11:11 OtpService — the library's one line
11:23 OtpService — the window and the replay memory
11:43 TotpSteps — the four steps
12:05 OtpAuthUri — the link
12:18 Secrets — never for a real account
12:26 Thanks for Watching
```

## Tags

totp, java-otp, otpauth, two-factor authentication, one-time password, rfc 6238, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/totp-multi-factor-with-java-otp-explained.mp4` |
| Captions | `video/totp-multi-factor-with-java-otp-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Passwords and MFA in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (47)
- [ ] tags are at most 500 characters in total — ok (158)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
