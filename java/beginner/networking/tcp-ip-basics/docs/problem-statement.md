# Problem Statement

## The scenario

An online store accepts connections from customers over the internet. Anyone can send it packets claiming any source, many at once, and watch the replies pass.

## The naive version

Assume that a connection from an address is from the owner of that address, that a checksum protects a packet, and that a stream is private because it is a connection.

## What this project must deliver

- An IPv4 packet and a TCP segment built and read byte by byte, with the RFC 1071 checksum.
- A demonstration that a checksum with no key does not stop an attacker.
- The handshake as a state machine, with a limited table of half-open entries.
- A SYN flood and the SYN cookie defence.
- A forged connection from guessable sequence numbers, and the keyed numbers that stop it.
- A real connection whose content is visible to an observer.
