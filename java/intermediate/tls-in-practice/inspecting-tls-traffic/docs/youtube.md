# YouTube sheet — Inspecting TLS Traffic with a Key Log

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

Inspecting TLS Traffic with a Key Log (SSLKEYLOGFILE) in Java

## Description

Inspecting TLS traffic in plain Java, with nothing hidden: a TLS 1.3 style key schedule (HKDF-SHA-256, X25519) and AES-GCM records, what a capture shows a network observer, the NSS key log format behind SSLKEYLOGFILE, a client that refuses to write one in production, a decryptor that opens exactly the connections and directions a key log lists, and forward secrecy: a leaked server key opens recorded static RSA sessions and nothing ephemeral.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/intermediate/tls-in-practice/inspecting-tls-traffic

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:32 The Words
02:10 What the Letters Stand For
03:11 What the Letters Stand For (continued)
04:15 What the Letters Stand For (continued)
04:23 One recording, three people
05:17 The seventy bytes of one record
06:18 Act One — What the wire shows
06:41 Act One — What the wire shows
07:02 One key log line, part by part
07:53 Act Two — Writing a key log
08:11 Act Two — Writing a key log
08:23 From the logged secret to the text
09:18 Act Three — Reading the capture with the key log
09:28 Act Three — Reading the capture with the key log
09:43 Three tries with a key log
10:27 Act Four — A key log opens its own connections and no others
10:46 One leaked key, two recordings
11:40 Act Five — Why the server's own key is a different matter
12:00 Act Five — Why the server's own key is a different matter
12:21 What It Costs
13:01 Where You Have Seen It
13:37 How To Use It
14:07 The Java Classes
14:22 KeyLog — one line per direction
14:34 KeyLog — refused in production
14:45 Secrets — from the shared secret to one secret per direction
15:02 Records — key, IV and nonce from the secret
15:19 Decryptor — a leaked server key, against each kind of connection
15:36 Thanks for Watching
```

## Tags

tls, sslkeylogfile, wireshark, key log, forward secrecy, hkdf, java, application security, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/inspecting-tls-traffic-explained.mp4` |
| Captions | `video/inspecting-tls-traffic-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: TLS in Practice in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (61)
- [ ] tags are at most 500 characters in total — ok (133)
- [ ] description is at most 5,000 characters — ok (445)
- [ ] chapters start at 00:00 and there are at least three
