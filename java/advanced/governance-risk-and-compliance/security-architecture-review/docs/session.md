# Session Guide — Security Architecture Review

## Learning Objectives

By the end of the session you can:

- describe a design as text with trust zones and flow properties
- write a rule over a design and score it on planted weaknesses
- generate STRIDE questions from a model
- tell what a model-based review cannot find

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The design as drawn | 7 min |
| 0:17 | Act 2: Rules over the design | 7 min |
| 0:24 | Act 3: One more rule | 7 min |
| 0:31 | Act 4: The reviewed design | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the first eight rules with the ninth. Then open design/v1/bank.dsl and find the flow that no cross-zone rule can see.

## Exercises

1. Add a rule that every internet-facing part has a web application firewall property.
2. Add a hidden-form-field note to the model as a trust assumption and write a rule that requires one for each web part.
3. Check that every decision record names its consequences.
4. Draw the container view with the Structurizr tools and compare it with the rules.
