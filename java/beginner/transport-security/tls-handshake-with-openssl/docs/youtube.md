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
02:02 What the Letters Stand For
02:42 What the Letters Stand For (continued)
03:15 One handshake, two implementations
03:57 One order over a real socket
04:45 Act One — A real TLS server and client on this machine
05:07 How a certificate becomes PEM text
06:07 Act Two — Giving the certificate and key to other programs
06:29 Act Two — Giving the certificate and key to other programs
06:46 Who answered? Two checks on every certificate
07:34 Act Three — Who is on the other end
07:55 Which versions connect
08:34 Act Four — Versions: what each side offers
08:53 openssl as the client: the command and what it prints
09:47 Act Five — Checking from the outside, and the bill
10:04 Act Five — Checking from the outside, and the bill
10:20 Why A Second Implementation
10:41 Where You Have Seen It
11:13 How To Use It
11:37 The Java Classes
11:46 TlsOpenSslDemo — the five acts
11:58 TlsServer — listen, handshake, answer one line
12:14 TlsClient — check the name, offer versions
12:27 Pem — Base64 in lines of 64, between labels
12:41 Base64Steps — 24 bits, four groups of 6
13:00 Thanks for Watching
```

## Tags

tls, openssl, s_client, s_server, pem, certificate verification, hostname verification, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tls-handshake-with-openssl-explained.mp4` |
| Captions | `video/tls-handshake-with-openssl-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS and Certificates in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (57)
- [ ] tags are at most 500 characters in total — ok (164)
- [ ] description is at most 5,000 characters — ok (576)
- [ ] chapters start at 00:00 and there are at least three
