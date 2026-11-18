# YouTube sheet — Inspecting TLS Traffic with Wireshark

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Inspecting TLS Traffic with Wireshark and tshark: Key Logs and Leaked Keys (OWASP A04)

## Description

A real nginx, the openssl client with a key log file, a tap that writes a capture file of a login, and tshark 4.6: what the capture shows without secrets (the server name, the cipher, encrypted records), what the key log opens (the request, the form, the cookie, the encrypted certificate), why one connection's key log opens no other, a log with only the client's lines, and the difference between a leaked server key on a recording made with ephemeral keys and one made with RSA key exchange.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/inspecting-tls-traffic-with-wireshark

Chapters:

```
00:00 Introduction
00:54 The Scenario
01:30 The Words
02:07 What the Letters Stand For
03:03 What the Letters Stand For (continued)
03:49 The same login, two ways
04:48 The first record, and the size of the login
06:04 Act One — What the wire shows
06:26 Act One — What the wire shows
06:47 One line of a key log, taken apart
08:00 Act Two — A key log: the secrets of one connection
08:13 Act Two — A key log: the secrets of one connection
08:39 Each secret opens its own part
09:40 Act Three — The same capture, read with the key log
10:01 A log matches by client random
11:03 Act Four — A key log opens its own connection and no other
11:16 Act Four — A key log opens its own connection and no other
11:32 Ephemeral keys, or a secret that travels
12:42 Act Five — The server's own key is a different matter
12:59 Act Five — The server's own key is a different matter
13:22 What It Costs
13:55 Where You Have Seen It
14:28 How To Use It
14:56 The Java Classes
15:15 ClientHello — the random and the server name
15:38 KeyLog — a log names connections by client random
15:56 Suites — ephemeral, or RSA
16:19 Tap — copy, then forward
16:37 Tshark — a key log, or the server's key
16:49 Client — write the secrets, or offer only RSA
17:06 Thanks for Watching
```

## Tags

wireshark, tshark, tls, key log, sslkeylogfile, tls 1.3, forward secrecy, owasp top 10, a04, java, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/inspecting-tls-traffic-with-wireshark-explained.mp4` |
| Captions | `video/inspecting-tls-traffic-with-wireshark-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (139)
- [ ] description is at most 5,000 characters — ok (494)
- [ ] chapters start at 00:00 and there are at least three
