# Problem Statement

## The scenario

An online store builds with Gradle from a private repository and a public one, and an attacker can publish anything under any name in the public one.

## The naive version

List both repositories and trust that the right one answers.

## What this project must deliver

- Real Gradle builds against two real repositories.
- The attacker's package winning by range and by order.
- Content filters and the request count that proves them.
- Dependency verification and where it stops.
