# Spec — TCP/IP Basics for Security

## Purpose

Teach what TCP/IP does and does not provide by building an IPv4 packet and a TCP segment, running the handshake as a state machine, flooding it, defending it, forging a connection and watching a real connection.

## Not in scope

- A full TCP implementation, with options, retransmission and congestion control.
- Raw sockets and real packet capture: the tool counterpart.
- Names and the web: the next projects.

## The problem

Learners use connections every day but do not know what is trusted in them, and so trust the wrong things.

## The pieces

| Role | Class |
| --- | --- |
| Internet checksum | `Checksum` |
| IPv4 packet | `Ipv4Packet` |
| TCP segment | `TcpSegment` |
| The server's handshake | `Listener` |
| Sequence numbers | `SequenceNumbers` |
| A real connection and a tap | `PlaintextTap` |
| Entry point | `TcpIpDemo` |

## Requirements

1. Packets round-trip, and any changed header byte is caught by the checksum.
2. The checksum matches the RFC 1071 example.
3. The handshake establishes only with the right acknowledgement, and a full table drops new SYNs.
4. SYN cookies keep no state and still establish connections.
5. Counter numbers can be guessed and keyed numbers cannot.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 19, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 07:20, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
