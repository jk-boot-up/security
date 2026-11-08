# YouTube sheet — TCP/IP Basics for Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TCP/IP Basics for Security in Java (Handshake, SYN Flood, Sequence Numbers)

## Description

TCP/IP basics for security in Java: packets, checksums, the three-way handshake, SYN floods and guessable sequence numbers. Explained with an online store. We build an IPv4 packet and a TCP segment byte by byte, see that a checksum catches accidents but not attackers, run the three-way handshake as a state machine, fill its table with a SYN flood and defend with SYN cookies, forge a connection by guessing sequence numbers, and watch a real connection carry a card number in the clear.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/tcp-ip-basics

Chapters:

```
00:00 Introduction
01:05 The Scenario
01:25 The Words
01:56 Act One — Layers: a packet inside a packet
02:29 Act Two — A checksum catches accidents, not attackers
02:53 Act Three — The three-way handshake
03:20 Act Four — A SYN flood
04:05 Act Five — Guessable numbers, and a stream anyone can read
04:42 What TCP/IP Gives, and Does Not
05:08 Who Does What
05:41 Where You Have Seen It
06:11 How To Use It
06:33 Thanks for Watching
```

## Tags

tcp, ip, three-way handshake, syn flood, syn cookies, sequence numbers, networking, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tcp-ip-basics-explained.mp4` |
| Captions | `video/tcp-ip-basics-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (75)
- [ ] tags are at most 500 characters in total — ok (160)
- [ ] description is at most 5,000 characters — ok (488)
- [ ] chapters start at 00:00 and there are at least three
