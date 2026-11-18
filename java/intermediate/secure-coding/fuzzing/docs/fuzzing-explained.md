# Fuzzing: Finding the Inputs Nobody Tested, Explained

## The idea in one sentence

Fuzzing runs a program on large numbers of mutated inputs, keeps the ones that reach new code, and records every failure other than an expected refusal, so that each crash can be shrunk to its smallest input, fixed and kept as a regression test.

## The 5 acts

### 1. The example tests pass

The input is six example tests: two order lines, a word where a number should be, two carts and a broken cart. The result: all 6 pass. SHO-1042,2,8990 splits into three fields, and the total is 2 x 8990 = 17980 cents.
Then the input nobody wrote a test for: "SHO-1042", a line with only a product code. split(",") gives one field, the code reads fields[1], and the result is an ArrayIndexOutOfBoundsException in OrderLine.parseCareless. A parser may refuse input; anything else it throws is a bug.

### 2. A mutation fuzzer

The fuzzer changes a seed input in one or two ways per run: it flips one bit, inserts, deletes or replaces a character, swaps a number for a boundary value (0, 1, 99, 100, 999999999, 2147483647, -1), or inserts 200000 opening brackets. Its random numbers come from L64X128MixRandom with seed 7, so every run is the same.
The first six runs show the input before and after the change; all six are refused. A flipped bit can make an invisible control character, printed as \x10.
In 20000 runs the order parser crashes twice: at run 86 an ArrayIndexOutOfBoundsException from "SHO-1042,28990", a line missing its comma, and at run 548 an IllegalArgumentException in Money from "BAG-0007,999999999,4500".
The overflow worked out: 999999999 x 4500 = 4499999995500; an int keeps the low 32 bits, 4499999995500 - 1047 x 4294967296 = 3169236588, and read as a signed int that is 3169236588 - 4294967296 = -1125730708, a negative total that Money refuses.
In 100000 runs the cart parser overflows its stack at run 1223, on 200039 characters that start with opening brackets.

### 3. Guided by coverage

The gift-message bug needs a field named note and a value starting with #. Blind, the fuzzer always changes a seed and keeps nothing: in 100000 runs it reaches 14 branches and never finds the bug.
Guided by coverage, it keeps every input that reaches a branch no earlier input reached: a comma in a list at run 1162, an escape at 1486, an empty object at 2142, an empty list at 3251, then "n" matched at run 21497, "no" at 23783, "not" at 24448 and "note" at 47709. That is 18 branches and 10 inputs kept (2 seeds and 8 new).
At run 49776 the input [{"note":"#AG-0007","qty"1}] crashes. How: the note is "#AG-0007", indexOf(':') is -1, and substring(1, -1) throws a StringIndexOutOfBoundsException in CartParser.giftMessage.

### 4. From crash to fix

Delta debugging removes pieces of a crashing input for as long as it still crashes the same way. The gift-message input shrinks 28, 21, 17, 16, 14, 13, 12 and 11 characters: {"note":"#".
Before the fix, the four smallest inputs crash the careless parsers: "SHO-1042" with an ArrayIndexOutOfBoundsException, "BAG-0007,999999999,4" with an IllegalArgumentException, {"note":"#" with a StringIndexOutOfBoundsException, and the 200039-character nesting with a StackOverflowError.
After the fix, the fixed parsers refuse each one: a line has three fields; quantity is 1 to 99; a gift message is #to:message; nested deeper than 32. Each smallest input is now a regression test; the nesting one is kept as it is, because how deep is too deep depends on the stack.
Under the same fuzzer the fixed parsers show 0 crashes in 20000 runs and 0 in 100000. A property check, parse, print and parse again, gives the same cart for 2000 of 2000 valid inputs; for example [{"sk":"BAG-0007","qty":1} ] prints without the space.

### 5. What fuzzing does not find

The input is a cart with a quantity of -5: {"sku":"SHO-1042","qty":-5}. The result, from the fixed parser: parsed. No crash, so no report.
Whether -5 is allowed is a business rule; a fuzzer finds crashes and broken properties, not wrong decisions.
What it costs: 2 harnesses, one for each parser; a fixed budget of runs on every change; longer runs at night, keeping the inputs that reached new code. Coverage-guided tools such as Jazzer add the branch counters to the bytecode by themselves, which is the -with-jazzer version.

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
