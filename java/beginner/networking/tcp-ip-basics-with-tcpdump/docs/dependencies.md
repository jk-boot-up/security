# Dependencies

This project adds no Java library. It uses two command-line tools, and only for the integration tests.

## What tcpdump is

the classic command-line packet capture and analysis tool, built on libpcap. The tests run `tcpdump -r` on the file the Java code wrote, to prove the format is the standard one.

## What Python is

runs `python/unfinished_handshakes.py`, written with the standard library only, as an independent second implementation of the flood check.

## Why this project uses them

The plain project modelled packets in memory. A tool that other people wrote, reading the file this project writes, is the proof that the model matches reality.

## What to install

A JDK for the demo and the ordinary tests. For the integration tests, tcpdump and Python 3 on the PATH (both are present on macOS and most Linux systems). The versions are pinned where Gradle controls them:

| Tool | Version |
| --- | --- |
| tcpdump | 4.99.1 |
| libpcap | 1.10.1 |
| Python | 3.14 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The integration tests need tcpdump and Python installed.
- The Java formatter copies tcpdump's line format for TCP only; tcpdump prints far more protocols.
