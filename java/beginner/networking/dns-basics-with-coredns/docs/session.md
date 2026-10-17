# Session Guide — DNS Basics with CoreDNS

## Learning Objectives

By the end of the session you can:

- read a Corefile and a zone file
- explain the difference between NXDOMAIN and REFUSED
- explain how UDP and TCP carry the same message
- explain why a cache keeps a wrong answer alive
- say what protects a name, since the server does not

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A real DNS server, in a container | 7 min |
| 0:17 | Act 2: The same question over UDP and over TCP | 7 min |
| 0:24 | Act 3: Names that do not exist, names it will not answer, and a rewrite | 7 min |
| 0:31 | Act 4: The cache and the countdown of the time to live | 7 min |
| 0:38 | Act 5: A zone with a wrong record | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the phone book. Run the demo one act at a time, and open the Corefile and the zone file the demo writes. After act three, ask what a client could do if the rewrite were wrong. After act five, ask who in the store can change the zone file, and how would anyone notice.

## Exercises

1. Add an AAAA record to the zone file and ask for it with `dig` while the container runs.
2. Change `cache 60` to `cache 5` and watch how the countdown changes.
3. Add a `forward` plugin block for a second zone that sends questions to the first server.
4. Run `dig +tcp @127.0.0.1 -p <port> www.shop.example` against the container and compare with the Java output.
