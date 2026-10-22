# Fuzzing with Jazzer, Explained

## The idea in one sentence

A fuzzer turns the examples you already have into thousands of inputs you did not think of, and keeps the ones that reach new code or break something.

## The 5 acts

### 1. The example tests pass

The order line parser passes six of its six example tests. SHO-1042,2,8990 gives a total of 17980 cents, and SHO-1042,two,8990 is refused as invalid. But a line with only a product code, SHO-1042, throws an ArrayIndexOutOfBoundsException: an input nobody wrote a test for. A parser may refuse input; anything else it throws is a bug.

### 2. The real Jazzer

Jazzer 0.30 is started as a program on a fuzz target of three lines, with the two example inputs as seeds, random seed 7 and 300000 runs. It adds branch counters to the parser's bytecode by itself and reports three distinct findings: ArrayIndexOutOfBoundsException in OrderLine.parseCareless at line 24 (second field missing), at line 25 (third field missing), and IllegalArgumentException in Money.<init> (a total that overflowed an int and went negative). Each input is saved in a file.

### 3. Guided by coverage, and by what you give it

With no seed inputs and 300000 runs, Jazzer reaches 7 coverage points and finds nothing: it cannot guess a product code. With the two example lines as seeds it reaches 19 and finds 3. The cart parser, 1000000 runs, with seeds that have no gift note, finds nothing; with one more seed that has a note field it finds the StringIndexOutOfBoundsException in CartParser.giftMessage. The seeds are the examples the team already has; the fuzzer mutates them, keeps what reaches new branches, and spends its runs near the interesting parts.

### 4. From crash to fix

Each saved input is made smallest while it still crashes in the same place: AAA-0000 (a line with one field), AAA-0000,0 (two fields) and AAA-0000,4000,4000000 (a total that overflows an int). The fixed parser refuses each with a reason. Each smallest input is now a regression test that runs on every build, in milliseconds, through the Jazzer JUnit integration. The fixed order parser under the same fuzzer: 0 findings in 300000 runs; the fixed cart parser: 0 in 1000000.

### 5. What fuzzing finds besides crashes, and what it does not

A search box that hands what the customer typed to the regular expression engine: Jazzer reports Regular Expression Injection in Parsers.searchCareless, a security issue and not a crash; the same box looking for the text as text has 0 findings. A cart with a quantity of -5 is parsed: no crash, so no report; whether -5 is allowed is a business rule. The cart's nesting crash needs more than Jazzer's default input length of 4096 bytes; written by hand, the careless parser overflows the stack and the fixed one refuses nesting deeper than 32. In a build, a fixed budget of runs on every change; longer runs at night.

## The verdict

Fuzz every parser that reads bytes or text from outside, seed it with your examples, run a fixed budget on every change and longer ones at night, make every finding smallest and keep it as a regression test, and remember that a fuzzer finds crashes and known security issues, not wrong decisions.

## How to recognise this in code you did not write

- `@FuzzTest` or `fuzzerTestOneInput`.
- A folder of saved inputs named after a test.
- A `crash-` file in a build.
- A nightly job with a time limit.

## Where you have already met this

- A `crash-...` file next to a build.
- `@FuzzTest` in a JUnit class.
- A `corpus` folder in a repository.
- A nightly job named `fuzz`.
