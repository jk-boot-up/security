# Spec — TCP/IP Basics with tcpdump and Python

## Purpose

Teach how to look at packets by writing a standard pcap file from Java, reading it with the real tcpdump and an independent Python script, and finding a flood and a damaged packet.

## Not in scope

- Live capture from an interface, which needs root.
- Protocols other than TCP over IPv4.
- Intrusion detection rules.

## The problem

Packets are abstract until you have seen them; learners need to see real ones, with a tool they will meet at work.

## The pieces

| Role | Class |
| --- | --- |
| pcap file format | `PcapFile` |
| The traffic | `Traffic` |
| Reading, flood and damage checks | `Capture` |
| IPv4 and TCP | `Ipv4Packet`, `TcpSegment` |
| Independent flood check | `python/unfinished_handshakes.py` |
| Entry point | `TcpdumpDemo` |

## Requirements

1. The capture file has the documented pcap layout and round-trips.
2. The Java lines equal the real tcpdump's lines for the same file.
3. The Python script and the Java code find the same unfinished handshakes.
4. A damaged packet is found, and tcpdump -v marks it incorrect.
5. The demo needs no tool and prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 06:26, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
