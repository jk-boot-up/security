# Problem Statement

## The scenario

An online bank is adding features every month: a mobile app, a gateway, a payments service, stores, and a card processor outside its walls. Each feature is reviewed for bugs, but nobody asks, in a repeatable way, how the design could be attacked, or checks that the answers still hold after the next change.

## The naive version

Leave security to code review and penetration tests at the end. They find bugs, but a design flaw, such as a flow that skips a boundary, is cheapest to fix before anything is built.

## What this project must deliver

- A data-flow model of the bank in code, with trust boundaries and a completeness check.
- STRIDE rules that generate candidate threats per element and boundary crossing.
- Ratings, and a register of decisions with owners and review dates.
- Mitigations as executable checks that fail when the design changes.
- The same facts printed on every run.
