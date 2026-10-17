# Problem Statement

## The scenario

An online bank's login is behind nginx. Guessing at it is cheap and fast, and the bank wants to hold an attacker to a few attempts without
turning away real customers.

## The naive version

Pass every request to the application and rely on the application to count failures. That means every guess reaches
the password check, and a limit written in the application cannot protect the application from the cost of being asked.

## What this project must deliver

- A real nginx, started in Docker for each act, with a configuration generated from Java.
- A client that sends twenty guesses as fast as it can and counts the answers.
- A limit on the client address that holds the attacker to one plus the burst.
- The same limit keyed on a client-written header, and the bypass it allows.
- A limit per username, its effect on the owner, and its blindness to a spray.
- Unit tests that need no Docker, and container tests that do.
