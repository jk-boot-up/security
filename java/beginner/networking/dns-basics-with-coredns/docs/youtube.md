# YouTube sheet — DNS Basics with CoreDNS

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

DNS Basics with CoreDNS (UDP, TCP, NXDOMAIN, Cache, Wrong Records)

## Description

DNS basics with CoreDNS, in Java: run a real DNS server in a container and ask it questions with our own code. Explained with an online store, like a phone book that prints whatever the last editor wrote. We query the server over UDP and TCP, see a name that does not exist, a zone it refuses to answer, a rewrite rule, the cache and the countdown of the time to live, and a zone with a wrong record that the server serves exactly like the right one, and finish with what really protects names: control of the zone file, and DNSSEC against answers forged on the way.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/dns-basics-with-coredns

Chapters:

```
00:00 Introduction
01:01 The Scenario
01:20 The Words
01:47 What the Letters Stand For
02:33 What the Letters Stand For (continued)
02:45 One question, one answer
03:35 The question and the answer, byte by byte
05:04 Act One — A real DNS server, in a container
05:28 Act One — A real DNS server, in a container
05:41 Act One — A real DNS server, in a container
05:52 One message, two ways to send it
06:47 Act Two — The same question over UDP and over TCP
07:10 Three questions, three kinds of answer
08:00 Act Three — Names that do not exist, names it will not answer, and a rewrite
08:20 The cache cuts the time to live, then counts it down
09:06 Act Four — The cache and the countdown of the time to live
09:28 Before and after a wrong change to the zone file
10:26 Act Five — A zone with a wrong record
10:47 Act Five — A zone with a wrong record
11:02 What Protects a Name
11:26 How To Use It
11:47 The Java Classes
11:56 CoreDnsDemo — two servers, five acts
12:13 ShopZone — the two files CoreDNS reads
12:38 CoreDns — docker run, on loopback only
12:57 Client — one datagram, or a length and a stream
13:14 DnsMessage — encode: header, name, record
13:34 WireSteps — the time to live, worked out
13:57 Thanks for Watching
```

## Tags

dns, coredns, docker, nxdomain, dns cache, ttl, zone file, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/dns-basics-with-coredns-explained.mp4` |
| Captions | `video/dns-basics-with-coredns-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (66)
- [ ] tags are at most 500 characters in total — ok (135)
- [ ] description is at most 5,000 characters — ok (566)
- [ ] chapters start at 00:00 and there are at least three
