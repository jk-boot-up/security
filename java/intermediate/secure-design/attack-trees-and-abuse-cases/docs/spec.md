# Spec — Attack Trees and Abuse Cases

## Purpose

Teach attack trees and abuse cases by building a tree for an online bank, computing the cheapest attack path, ranking and sequencing defences by what they do to it, and running abuse cases that fail until the defences exist.

## Not in scope

- A graphical tool or file format for attack trees.
- Formal probabilistic analysis with shared leaves.
- The pytm and Threat Dragon versions: the attack-trees-and-abuse-cases-with-threat-dragon project.

## The problem

Security spending follows fashion and fear, and nobody has priced the attacker's alternatives.

## The pieces

| Role | Class |
| --- | --- |
| The tree | `Node`, `Analysis`, `BankTree` |
| Defences and planning | `Defence`, `Planner` |
| Abuse cases | `AbuseCase`, `BankConfig` |
| Entry point | `AttackTreeDemo` |

## Requirements

1. Costs add along AND and the cheapest child wins at OR; chances multiply along AND and combine at OR; a step with no chance blocks its branch.
2. The baseline tree has a cheapest path of 50 EUR and a success chance of 97%.
3. No single defence fixes the tree; the ownership check is the only one that moves the cheapest path.
4. The greedy plan is OWN, CSRF, PASSKEY, CSP and reaches a cheapest path of 6,000 EUR for 55,000 EUR.
5. Every abuse case fails on the weak bank and passes with all defences; 13 of 14 steps have one.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 66 lines, identical on every run |
| Acts | 5 |
| Video | 06:46, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
