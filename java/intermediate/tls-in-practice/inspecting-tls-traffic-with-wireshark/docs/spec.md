# Spec — Inspecting TLS Traffic with Wireshark

## Purpose

Teach what a TLS capture shows and what opens it, with real tools and a tap that writes the capture.

## Not in scope

- Capturing from a network card.
- Breaking TLS.
- Decrypting QUIC.

## The problem

People either think a TLS recording is useless or that it can be read at will; both are wrong in specific ways.

## The pieces

| Role | Class |
| --- | --- |
| The tap | `Tap`, `Pcap` |
| The servers | `Nginx`, `LegacyServer` |
| The client | `Client` |
| The reader | `Tshark` |
| Entry point | `TlsCaptureDemo` |

## Requirements

1. A tap forwards a connection and writes a valid capture file.
2. tshark reads the capture with and without secrets.
3. A key log opens only its own connection.
4. A server key opens an RSA key exchange recording and not an ECDHE one.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 05:25, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
