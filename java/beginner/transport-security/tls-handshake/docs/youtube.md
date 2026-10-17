# YouTube sheet — The TLS Handshake

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

The TLS Handshake Explained in Java (TLS 1.3, Certificates, Tampering)

## Description

The TLS handshake in Java: how two strangers agree on a secret in public. Explained with an online store, like two people who must settle on a code word while a stranger listens. Using two real TLS engines wired together in memory, we watch a sniffer see a card number without TLS and only scrambled records with it, follow the handshake from the readable ClientHello to the encrypted rest, see a client refuse an impostor and a wrong site name, see that one changed bit in a record is refused, see that old protocol versions are switched off, and finish with the bill.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/transport-security/tls-handshake

Chapters:

```
00:00 Introduction
01:05 The Scenario
01:24 The Words
01:53 Act One — A sniffer on the wire, without and with TLS
02:19 Act Two — The handshake
02:58 Act Three — Who is on the other end
03:30 Act Four — Protecting the data
04:08 Act Five — Settings, and the bill
04:46 What TLS Gives, and Does Not
05:11 Who Does What
05:34 Where You Have Seen It
05:59 How To Use It
06:22 Thanks for Watching
```

## Tags

tls, tls 1.3, handshake, ssl, sslengine, certificates, https, java, cybersecurity for beginners

## Files

| What | File |
| --- | --- |
| Video | `video/tls-handshake-explained.mp4` |
| Captions | `video/tls-handshake-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (87)
- [ ] description is at most 5,000 characters — ok (569)
- [ ] chapters start at 00:00 and there are at least three
