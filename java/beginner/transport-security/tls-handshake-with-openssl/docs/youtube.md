# YouTube sheet — The TLS Handshake with OpenSSL

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

The TLS Handshake with OpenSSL (s_client, s_server, Java)

## Description

The TLS handshake with OpenSSL, in Java: the best way to trust your TLS setup is to point a second implementation at it. Explained with an online store, like asking a stranger to read your map back to you. We run a real TLS server and client on one machine, write the certificate and key as PEM files that other programs read, see the client refuse an impostor and a wrong name, offer old and new protocol versions, and use openssl s_client and s_server in the tests as the other end of the conversation, and finish with the bill: openssl only checks what you ask it to check.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/tls-handshake-with-openssl

Chapters:

```
00:00 Introduction
01:11 The Scenario
01:30 The Words
02:02 Act One — A real TLS server and client on this machine
02:30 Act Two — Giving the certificate and key to other programs
02:59 Act Three — Who is on the other end
03:28 Act Four — Versions: what each side offers
04:02 Act Five — Checking from the outside, and the bill
04:39 Why A Second Implementation
05:00 Who Does What
05:30 Where You Have Seen It
06:02 How To Use It
06:26 Thanks for Watching
```

## Tags

tls, openssl, s_client, s_server, pem, certificate verification, hostname verification, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/tls-handshake-with-openssl-explained.mp4` |
| Captions | `video/tls-handshake-with-openssl-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (57)
- [ ] tags are at most 500 characters in total — ok (113)
- [ ] description is at most 5,000 characters — ok (576)
- [ ] chapters start at 00:00 and there are at least three
