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
01:32 The Scenario
01:49 Act One — What the wire shows
02:05 Act Two — A key log: the secrets of one connection
02:20 Act Three — The same capture, read with the key log
02:38 Act Four — A key log opens its own connection and no other
02:56 Act Five — The server's own key is a different matter
03:14 What It Costs
03:38 Who Does What
04:00 Where You Have Seen It
04:12 How To Use It
04:32 Thanks for Watching
```

## Tags

wireshark, tshark, tls, key log, sslkeylogfile, tls 1.3, forward secrecy, owasp top 10, a04, java

## Files

| What | File |
| --- | --- |
| Video | `video/inspecting-tls-traffic-with-wireshark-explained.mp4` |
| Captions | `video/inspecting-tls-traffic-with-wireshark-explained.srt` |
| Thumbnail | `docs/thumbnail.png` (1280×720) |

## Settings

Public with instant Premiere; not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Security Foundations in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (86)
- [ ] tags are at most 500 characters in total — ok (88)
- [ ] description is at most 5,000 characters — ok (494)
- [ ] chapters start at 00:00 and there are at least three
