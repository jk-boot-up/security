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
01:53 What the Letters Stand For
02:38 What the Letters Stand For (continued)
02:57 One order, three jobs of TLS
03:41 What the sniffer sees, before and after TLS
04:45 Act One — A sniffer on the wire, without and with TLS
05:12 The handshake: who sends what, in order
06:20 Act Two — The handshake
06:47 Act Two — The handshake
07:03 Who answered? Two checks on every certificate
07:50 Act Three — Who is on the other end
08:15 One flipped bit, and the tag check
08:58 Act Four — Protecting the data
09:27 Which versions connect, and the bill
10:16 Act Five — Settings, and the bill
10:34 What TLS Gives, and Does Not
10:59 Where You Have Seen It
11:24 How To Use It
11:47 The Java Classes
11:55 TlsDemo — the five acts
12:05 TlsDemo — act four: flip one bit
12:16 Connection — a client engine and a server engine
12:29 Connection — the handshake loop, and sealing data
12:44 Wire — reading record headers
12:56 TestCertificates — trust one certificate; its fingerprint
13:12 Thanks for Watching
```

## Tags

tls, tls 1.3, handshake, ssl, sslengine, certificates, https, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-handshake-explained.mp4` |
| Captions | `video/tls-handshake-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (70)
- [ ] tags are at most 500 characters in total — ok (138)
- [ ] description is at most 5,000 characters — ok (569)
- [ ] chapters start at 00:00 and there are at least three
