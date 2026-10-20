# Session Guide — Attack Trees and Abuse Cases

## Learning Objectives

By the end of the session you can:

- read an attack tree and find its cheapest path
- explain how costs and chances combine at AND and OR gates
- say why one defence rarely fixes a tree, and why you recompute after each
- write an abuse case that fails now and passes when the defence exists

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The tree | 7 min |
| 0:17 | Act 2: What the tree says about the attacker | 7 min |
| 0:24 | Act 3: Each defence on its own | 7 min |
| 0:31 | Act 4: A plan: always cut the cheapest path, then look again | 7 min |
| 0:38 | Act 5: Abuse cases as tests | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask which branch the team has probably been defending. In act three, ask why the passkey did not move the cheapest path. In act four, ask which defence would be next, and what it costs. In act five, ask what a test for the unchecked step would look like.

## Exercises

1. Add a branch for the support desk resetting a password for a caller, and decide its cost and chance.
2. Share a leaf between two branches (the leaked password) and see how the success chance changes.
3. Replace the greedy plan with a search over subsets under a budget of 30,000 EUR.
4. Write a real abuse case against the transfer endpoint of the broken-access-control project.
