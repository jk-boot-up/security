# Foundations

The first module. It asks the question every later project builds on: what is a system trying to protect, and
from whom?

## The projects

| # | Project | Scenario |
| --- | --- | --- |
| 1 | [cia-triad](cia-triad) | An online store's orders fail confidentiality, integrity and availability, one control at a time |
| 2 | [threats-and-risk](threats-and-risk) | An online store's attack surface, seven scored risks, a budgeted plan, and why a score is a judgment |

Each project builds on the one before. Project 1 introduces the three promises; project 2 uses them to rank what
could go wrong.

## How the module is judged

- **One system, broken on purpose.** Each project shows the naive version failing before any control appears.
- **Every promise is a number.** Each failure and each fix prints an exact result that a test checks.
- **The bill is part of the lesson.** Every control has a cost, and the last act of each project shows it.

The shared decisions are in [`docs/spec.md`](docs/spec.md) and [`docs/implementation-plan.md`](docs/implementation-plan.md).
