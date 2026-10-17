# Problem Statement

## The scenario

An online store wants to understand what its server really received during a purchase and during a flood, without taking the server down or guessing.

## The naive version

Read logs, which show what the application saw and not what the network delivered.

## What this project must deliver

- A pcap capture file written from Java, byte for byte in the standard format.
- The lines of tcpdump's output reproduced in Java, and checked against the real tool.
- A flood detector in Java and an independent one in Python that agree.
- A damaged packet found by its checksum.
- A demonstration that a capture holds secrets.
