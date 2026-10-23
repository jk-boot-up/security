# Spec — Risk Management

## Purpose

Teach quantitative risk management by simulating a bank's register, choosing controls within a budget and checking the residual risk against an appetite.

## Not in scope

- Full FAIR taxonomy and tooling.
- Fitting distributions to incident data.
- Correlated risks (suggested as an exercise).

## The problem

Risk registers rank by feeling and controls are bought without a price on the loss they avoid.

## The pieces

| Role | Class |
| --- | --- |
| A three-point estimate | `Tri` |
| A line of the register | `Risk` |
| A treatment | `Control` |
| The simulated years | `Simulation` |
| Choosing within a budget | `Optimizer` |
| Entry point | `RiskManagementDemo` |

## Requirements

1. Each risk has a range for frequency and for the loss of an event.
2. The simulation uses common random numbers so that treatments are compared on the same years.
3. The optimizer tries every set of controls within the budget.
4. The residual risk is compared with an appetite stated as a number.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 5 |
| Video | 05:50, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
