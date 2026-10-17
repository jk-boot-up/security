# Session Guide — TCP/IP Basics with tcpdump and Python

## Learning Objectives

By the end of the session you can:

- describe the pcap file format
- read a line of tcpdump output
- find an unfinished handshake in a capture
- say why a capture file needs protection

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Recording traffic to a capture file | 7 min |
| 0:17 | Act 2: Reading it back, in the style of tcpdump | 7 min |
| 0:24 | Act 3: Finding a SYN flood in a capture | 7 min |
| 0:31 | Act 4: Finding a damaged packet | 7 min |
| 0:38 | Act 5: What a capture reveals | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the security camera. Run the demo, then open the capture with the real tcpdump and match the lines. Ask what in the capture would be safe if TLS were used, and what would still show.

## Exercises

1. Open the capture in Wireshark and find the same flags.
2. Add a filter to the Java reader that prints only packets to port 443.
3. Change the flood to come from many networks and see what the Python script reports.
4. Run `tcpdump -nn -t -S -v -r` and find the checksum line for the damaged packet.
