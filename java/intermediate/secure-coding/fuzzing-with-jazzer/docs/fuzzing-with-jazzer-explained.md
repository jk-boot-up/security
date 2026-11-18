# Fuzzing with Jazzer, Explained

## The idea in one sentence

A fuzzer turns the examples you already have into thousands of inputs you did not think of, and keeps the ones that reach new code or break something.

## The 5 acts

### 1. The example tests pass

The input is six example tests for the order line parser: three lines with totals, a word where a number should be, a product code in small letters and an empty line. The result: 6 of 6 pass. SHO-1042,2,8990 splits into three fields, and the total is 2 x 8990 = 17980 cents.
Then the input nobody wrote a test for, "SHO-1042": the result is an ArrayIndexOutOfBoundsException at line 24 of OrderLine. A parser may refuse input; anything else it throws is a bug.

### 2. The real Jazzer

The input: Jazzer 0.30, started as a program of its own, on the fuzz target OrderLineCareless of three lines, with the seeds "SHO-1042,2,8990" and "BAG-0007,1,4500", 300000 runs, random seed 7 and inputs of up to 4096 bytes. Jazzer adds the coverage counters to the bytecode by itself.
The result: 3 distinct findings, an ArrayIndexOutOfBoundsException in OrderLine.parseCareless at line 24, another at line 25, and an IllegalArgumentException in Money.<init> at line 8.
Which line is which, worked out with two lines written by hand: "SHO-1042" has 1 field and crashes at line 24, which reads fields[1]; "SHO-1042,2" has 2 fields and crashes at line 25, which reads fields[2].
How a finding is named: the kind comes from the "== Java Exception" line of Jazzer's report, the place from the first frame in the store's code, repeats share one DEDUP_TOKEN, and the input is saved in a crash- file.

### 3. Guided by coverage, and by what you give it

With no seed inputs, 300000 runs reach 7 coverage points and find 0 bugs: the parser refuses everything before the product code is right.
How likely a random product code is: the target reads ASCII, 128 values a character. Three capitals are 26 of 128 each, (26/128)^3; a dash is 1 of 128; four digits are 10 of 128 each, (10/128)^4. Together, 128^8 / (26^3 x 10^4): one product code in 409977207 random strings of eight characters, far more than 300000 runs.
With the two example lines as seeds, the same runs reach 19 coverage points and 3 findings.
The cart parser, 1000000 runs from the seeds "[]" and a cart without a note, finds 0. With one more seed that has a note field, it finds a StringIndexOutOfBoundsException in CartParser.giftMessage at line 150.

### 4. From crash to fix

How the minimizer works, on "SHO-1042,28990": it deletes stretches of characters while the crash stays the same (shorter), then replaces capitals with A and digits with 0 (plainer). It ends at "AAA-0000,0".
Before the fix, Jazzer's three saved inputs, made smallest: "AAA-0000" crashes at line 24, "AAA-0000,0" at line 25, and "AAA-0000,4000,4000000" with an IllegalArgumentException at line 8.
How the overflow is made: 4000 x 4000000 = 16000000000; an int keeps the low 32 bits, 16000000000 - 3 x 4294967296 = 3115098112, and as a signed int that is 3115098112 - 4294967296 = -1179869184, a negative total Money refuses.
After the fix, the fixed parser refuses each: a line has three fields (twice), and quantity is 1 to 99. Each one is a regression test, run on every build in milliseconds. Under the same fuzzer the fixed order parser shows 0 findings in 300000 runs, and the fixed cart parser 0 in 1000000.

### 5. What fuzzing finds besides crashes, and what it does not

The careless search box hands the customer's words to the regular expression engine. Jazzer's sanitizer reports a Regular Expression Injection in Parsers.searchCareless at line 43; the query "(" throws a PatternSyntaxException. The fixed box looks for the words as text: 0 findings, and "(" is simply not found. A sanitizer reports a kind of vulnerability, not only a crash.
The input, a cart with a quantity of -5: the fixed parser's result is parsed. No crash, so no report: whether -5 is allowed is a business rule.
The nesting crash: Jazzer's inputs stop at 4096 bytes by default; written by hand, 100000 opening brackets give a StackOverflowError in the careless parser, and the fixed parser refuses them as nested deeper than 32.

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
