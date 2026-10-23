# Problem Statement

## The scenario

A bank wants to know when its payments pod starts doing something it never does.

## The naive version

Rely on admission control and network policies, and find out about a taken-over container from a customer.

## What this project must deliver

- A real cluster with a payments pod and a ledger pod.
- Three rules in Falco's language, checked by Falco.
- A sensor on this kernel that applies the same three tests.
- Normal work with 0 alerts, the three behaviours with 1 alert each, and prevent versus detect.
