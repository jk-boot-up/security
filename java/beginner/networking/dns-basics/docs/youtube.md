# YouTube sheet — DNS Basics for Security

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

DNS Basics for Security in Java (Messages, Caching, Cache Poisoning)

## Description

DNS basics for security in Java: messages, caching, and cache poisoning. Explained with an online store. We build a DNS message byte by byte with name compression, cache answers with a time to live, run a real exchange over UDP, poison a resolver's cache by guessing a transaction id, and see why random ids and ports, capped lifetimes, DNSSEC and TLS each stop a part of the attack.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/dns-basics

Chapters:

```
00:00 Introduction
00:41 The Scenario
00:59 The Words
01:26 What the Letters Stand For
01:52 What the Letters Stand For (continued)
02:21 One DNS question, byte by byte
03:26 The question and the answer
04:25 Act One — A DNS message, byte by byte
04:33 Act One — A DNS message, byte by byte
04:43 How long an answer is kept
05:29 Act Two — The cache and the time to live
05:36 One datagram out, one back
06:20 Act Three — A real exchange over UDP
06:27 Guessing the id and port
07:31 Act Four — Cache poisoning by guessing
07:45 The same lie under two caps
08:33 Act Five — How long a false answer lives, and what stops it
08:45 Where You Have Seen It
09:08 How To Use It
09:35 The Java Classes
09:39 DnsDemo — the five acts
09:48 DnsMessage — the header: six 2-byte fields
10:00 DnsMessage — a name: labels, or a pointer
10:12 Resolver — the checks, then the cache
10:28 TransactionIds — counting, or keyed
10:41 LoopbackExchange — one datagram each way
10:49 Address — four numbers into one
10:54 BadMessageException — refuse, do not guess
11:02 Thanks for Watching
```

## Tags

dns, cache poisoning, dnssec, resolver, transaction id, udp, networking, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/dns-basics-explained.mp4` |
| Captions | `video/dns-basics-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - Without Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (68)
- [ ] tags are at most 500 characters in total — ok (149)
- [ ] description is at most 5,000 characters — ok (383)
- [ ] chapters start at 00:00 and there are at least three
