# Problem Statement

## The scenario

An online store keeps an account for every customer, and must check a password at every login
without being able to read it. Its accounts table will, sooner or later, be copied by someone who
should not have it.

## The naive version

Store what the customer typed. The next improvement, a plain hash, looks safe but gives identical
values for identical passwords and falls to a table of common passwords prepared in advance.

## What this project must deliver

- Four storage schemes behind one interface, so they can be compared.
- An attacker with a list of eight common passwords, and the exact work each scheme costs it.
- Work counted in steps, so the numbers are the same on every machine.
- A stored format that carries the algorithm, rounds and salt, so rounds can be raised later.
- An upgrade of old hashes after a correct login.
- The same output every run, checked against a golden output file.
