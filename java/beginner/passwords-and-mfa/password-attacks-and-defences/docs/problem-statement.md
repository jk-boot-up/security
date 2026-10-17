# Problem Statement

## The scenario

An online bank lets customers sign in from anywhere. Its passwords are stored well, but its login page answers every guess, at any speed, from any
address. It must slow and stop guessing without locking real customers out.

## The naive version

Answer every attempt, and tell the person exactly what was wrong. It is friendly and honest, and it hands a patient attacker unlimited
tries and a list of which usernames exist.

## What this project must deliver

- Three attack scripts, dictionary, spraying and stuffing, run against the same login.
- Four defences that can be switched on independently: lockout, rate limit, second factor and breach check.
- A clock made of ticks, so every number is repeatable.
- A count, for each defence, of what the attack achieved.
- A demonstration that a lockout can be used against the customer, and that messages can leak.
- The same output every run, checked against a golden output file.
