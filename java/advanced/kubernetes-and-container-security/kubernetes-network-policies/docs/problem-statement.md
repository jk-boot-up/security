# Problem Statement

## The scenario

A bank wants its database reachable only from the api, and wants to know that it is.

## The naive version

Write a few policies from an example and assume they work.

## What this project must deliver

- A real cluster with five pods.
- Default deny and one allow for each line of the design.
- A measurement of all pairs against the design.
- The mistakes that are accepted silently, egress and DNS, and labels.
