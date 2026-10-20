# Problem Statement

## The scenario

A shop reads order files from suppliers, and anyone who can send a file to the shop can send any bytes.

## The naive version

The parser passed its tests on the suppliers' example files, so it is safe.

## What this project must deliver

- A parser with five planted bugs and a fixed parser.
- A fuzzer with three strategies, an oracle, deduplication, a minimizer and a regression test generator.
- Hand-placed coverage instrumentation and a step budget as a deterministic timeout.
- A comparison of the strategies, with a fixed seed.
