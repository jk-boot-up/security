# Session Guide — TCP/IP Basics for Security

## Learning Objectives

By the end of the session you can:

- describe what an IP packet and a TCP segment contain
- explain why a checksum is not a security measure
- describe the three-way handshake and what a SYN flood abuses
- explain why sequence numbers must be unpredictable
- say what TCP does not provide

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Layers: a packet inside a packet | 7 min |
| 0:17 | Act 2: A checksum catches accidents, not attackers | 7 min |
| 0:24 | Act 3: The three-way handshake | 7 min |
| 0:31 | Act 4: A SYN flood | 7 min |
| 0:38 | Act 5: Guessable numbers, and a stream anyone can read | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the postal analogy. Run the demo one act at a time. After act two, ask who could recompute the checksum. After act four, ask what the store has lost besides connections.
In act five, ask what would have to be true for a source address to be trusted, and lead to the next projects: names, the web and TLS.

## Exercises

1. Add the acknowledgement of data, so that a payload is delivered after the handshake, and check what a forged ACK does.
2. Change the table size and the number of flooding SYNs and find the point at which a real customer gets in.
3. Make the counter step 1 instead of 64000 and check that the forgery still works.
4. Add a timeout to half-open entries, in simulated ticks, and see how it changes the flood.
