# Fuzzing: Finding the Inputs Nobody Tested, Explained

## The idea in one sentence

Fuzzing runs a program on large numbers of mutated inputs, keeps the ones that reach new code, and records every failure other than an expected refusal, so that each crash can be shrunk to its smallest input, fixed and kept as a regression test.

## The 5 acts

### 1. The example tests pass

The store's two parsers, for order import lines and for carts, pass all 6 of their 6 example tests: SHO-1042,2,8990 gives a total of 17980 cents, and SHO-1042,two,8990 is refused as invalid.
But a line with only a product code, "SHO-1042", crashes the order parser with an ArrayIndexOutOfBoundsException: an input nobody wrote a test for. A parser may refuse input; anything else it throws is a bug.

### 2. A mutation fuzzer

The fuzzer flips bits, inserts, deletes and replaces characters, swaps numbers for boundary values and nests brackets deeply, with random numbers from L64X128MixRandom and seed 7, so every run finds the same crashes in the same order.
In 20000 runs from 2 seed inputs, the order parser shows 2 distinct crashes: at run 86 an ArrayIndexOutOfBoundsException from "SHO-1042,28990", a line missing its comma, and at run 548 an IllegalArgumentException
from "BAG-0007,999999999,4500", where the quantity times the price overflows an int into a negative total. In 100000 runs the cart parser shows 1 distinct crash: at run 1223 a StackOverflowError from an input of
200039 characters starting with a wall of opening brackets. Each crash is a bug the example tests missed, found without anyone guessing the input.

### 3. Guided by coverage

The gift-message bug needs a field named note and a value starting with #: four letters and one more, all right at once. Blind, in 100000 runs, the fuzzer reaches 14 branches and never finds it.
Guided by coverage, keeping every input that reaches a new branch, it reaches 18 branches with 10 inputs kept, and finds the bug at run 49776 with the input [{"note":"#AG-0007","qty"1}]. The parser compares the
field name one letter at a time, so each letter matched is a new branch, and the fuzzer keeps the input that reached it. The work is counted in runs, not seconds, so the comparison is the same on every machine.

### 4. From crash to fix

Delta debugging removes pieces of each crashing input for as long as it still crashes the same way. The missing-field crash shrinks from 14 characters to "SHO-1042", which the fixed parser refuses because a line has three fields.
The overflow shrinks from 23 characters to "BAG-0007,999999999,4", refused because the quantity is 1 to 99. The gift-message crash shrinks from 28 characters to {"note":"#", refused because a gift message is #to:message.
The nesting crash is kept as it is, because how deep is too deep depends on the stack; the fixed parser refuses it as nested deeper than 32. Each smallest input is now a regression test. Under the same fuzzer the
fixed parsers show 0 crashes in 20000 runs and 0 in 100000, and a property check, parse, print and parse again, gives the same cart for 2000 of 2000 inputs the fuzzer found valid.

### 5. What fuzzing does not find

A cart with a quantity of -5 parses: no crash, so no report. Whether -5 is allowed is a business rule; a fuzzer finds crashes and broken properties, not wrong decisions. Every parser needs its own harness, and
this store wrote 2, one for each. In a build, a fixed budget of runs on every change; longer runs at night, keeping the inputs that reached new branches. Coverage-guided tools such as Jazzer add the branch counters
to the bytecode by themselves, which is the -with-jazzer version.

## The verdict

Fuzz every parser that reads outside input. Write one harness per parser that treats only the parser's own refusal as expected. Keep stack traces, seed the fuzzer from real inputs, and let coverage
guide it. Shrink every crash, keep the smallest input as a test, fix the cause, and fuzz again. Give every build a budget of runs, run longer at night, and use properties to catch wrong answers as well as crashes.

## How to recognise this in code you did not write

- `@FuzzTest` methods and Jazzer in Java projects.
- OSS-Fuzz badges and "found by fuzzing" in advisories.
- A `corpus` folder of interesting inputs next to a test.
- Regression tests named after a crash, with a strange-looking input.

## Where you have already met this

- OSS-Fuzz, which fuzzes thousands of open-source projects continuously.
- Jazzer and JUnit's `@FuzzTest` for Java, and libFuzzer and AFL++ for C and C++.
- Security advisories that begin "found by fuzzing".
- Property-based testing libraries such as jqwik, which generate inputs from rules.
