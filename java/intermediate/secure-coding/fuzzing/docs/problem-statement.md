# Problem Statement

## The scenario

An online store parses order lines from its warehouse and carts from its site. Both parsers pass the tests their authors wrote, and both read input from outside the store.

## The naive version

Write a handful of example tests for the inputs that come to mind, and treat a green test run as proof that the parser is safe.

## What this project must deliver

- Two parsers in a careless and a fixed version, instrumented with branch counters.
- A seeded mutation fuzzer, blind and guided by coverage, that records crashes by type and place.
- A delta-debugging minimiser that shrinks each crash to its smallest input.
- Regression tests made from the smallest inputs, and the fixed parsers fuzzed again.
- A property check that parsing and printing agree.
