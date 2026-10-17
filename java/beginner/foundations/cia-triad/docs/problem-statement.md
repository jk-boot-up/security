# Problem Statement

## The scenario

An online store keeps a record for every order. The order system will be used by
finance and support staff, every day, on information that is private, that must
be exactly right, and that must be there when a customer wants to buy.

## The naive version

The first version stores the orders in a map and answers every request for
every user in full. Anyone can read anything, anyone can change anything
without a trace, and the server does its work in the order requests arrive, so
whoever sends the most requests gets the most service.

## What this project must deliver

- A version that fails all three promises, with a visible result for each failure.
- A role check so that each role sees only what it needs (confidentiality).
- A seal on every order, checked on every read (integrity).
- A rate limit so that one client cannot use up the server (availability).
- An emergency exception that shows why the promises pull against each other.
- The same numbers every run, checked against a golden output file.
