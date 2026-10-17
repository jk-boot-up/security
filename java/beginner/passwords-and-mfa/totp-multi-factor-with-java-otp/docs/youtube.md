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
01:13 The Scenario
01:37 What the Library Offers
01:58 Act One — The library makes the same codes as the hand-written version
02:47 Act Two — What the library checks, and what it leaves to the bank
03:37 Act Three — Other hashes and longer codes
04:35 Act Four — Giving the secret to the phone
05:12 Act Five — The bill: what the library does not do
05:52 Library, and Bank
06:20 Who Does What
06:51 Where You Have Seen It
07:21 How To Use It
07:45 Thanks for Watching
```

## Tags

totp, java-otp, otpauth, two-factor authentication, one-time password, rfc 6238, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/totp-multi-factor-with-java-otp-explained.mp4` |
| Captions | `video/totp-multi-factor-with-java-otp-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (47)
- [ ] tags are at most 500 characters in total — ok (107)
- [ ] description is at most 5,000 characters — ok (638)
- [ ] chapters start at 00:00 and there are at least three
