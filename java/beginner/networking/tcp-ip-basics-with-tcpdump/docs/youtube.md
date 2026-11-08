# YouTube sheet — TCP/IP Basics with tcpdump and Python

Prepared text for YouTube Studio. **Nothing is uploaded until the author has reviewed the video and asks for it.**

## Title

TCP/IP Basics with tcpdump and Python (pcap, SYN Flood, Checksums)

## Description

TCP/IP basics with tcpdump and Python, in Java: packets are easier to understand when you can look at them. Explained with an online store, like a security camera recording at the shop door. We write a capture file in the standard pcap format, have the real tcpdump tool print the same lines as our Java code, find a SYN flood in a capture with a short independent Python script, spot a packet with a bad checksum, and finish with the bill: a capture file holds the card number in the clear, so it must be protected like the traffic itself.

Source code and notes: github.com/jk-boot-up/security/tree/main/java/beginner/networking/tcp-ip-basics-with-tcpdump

Chapters:

```
00:00 Introduction
00:59 The Scenario
01:18 The Words
01:42 What the Letters Stand For
02:22 What the Letters Stand For (continued)
02:47 What is inside one packet
03:32 Before and after recording: packets into a pcap file
04:17 Act One — Recording a purchase to a capture file
04:35 The handshake and the order, packet by packet
05:27 Act Two — Reading it back, in the style of tcpdump
05:48 Act Two — Reading it back, in the style of tcpdump
06:00 Who started a handshake, and who finished it
06:43 Act Three — Finding a SYN flood in a capture
07:06 How the TCP checksum is made
08:03 Act Four — How a checksum is made, step by step
08:22 Before and after the damage
09:06 Act Five — Finding a damaged packet
09:22 What the customer sent, and what the capture holds
10:03 Act Six — What a capture reveals
10:20 Where You Have Seen It
10:45 How To Use It
11:11 The Java Classes
11:21 Traffic — one packet: a TCP segment inside an IP packet
11:40 PcapFile — write: a file header, then a record header per packet
11:56 Capture — handshakes: who started, who finished
12:12 ChecksumSteps — split, add, fold, flip
12:26 TcpSegment — encode: the checksum goes into bytes 16 and 17
12:37 Thanks for Watching
```

## Tags

tcpdump, pcap, wireshark, syn flood, tcp, packet capture, python, java, cybersecurity for beginners, Jayasekhar Konduru, github.com/jk-boot-up/security/

## Files

| What | File |
| --- | --- |
| Video | `video/tcp-ip-basics-with-tcpdump-explained.mp4` |
| Captions | `video/tcp-ip-basics-with-tcpdump-explained.srt` |
| Thumbnail | `docs/thumbnail-security-photo-cyan-theme.png` (1280×720) |

## Settings

Uploaded Unlisted and made Public once the quality check passes (`tools/ytupload/ytquality.sh`); not made for kids; no paid promotion; altered content: No; category Science & Technology; language English; playlist: Networking for Security in Java - With Tools. Never announce a next video.

## Checks before saving

- [ ] description has no `http://`, `https://`, `<` or `>` — ok
- [ ] title is at most 100 characters — ok (66)
- [ ] tags are at most 500 characters in total — ok (142)
- [ ] description is at most 5,000 characters — ok (540)
- [ ] chapters start at 00:00 and there are at least three
