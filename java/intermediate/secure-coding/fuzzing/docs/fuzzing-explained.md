# Fuzzing a Parser, Explained

## The idea in one sentence

Fuzzing makes the inputs nobody thought of, reports anything but the allowed failure, shrinks each finding, and keeps it as a test; feedback from coverage reaches deeper than random or simple mutation, and a clean run is a search result, not a proof.

## The 4 acts

### 1. The parser works on the files it was written for

The supplier's two example files, a single order line and a longer file with a comment, a discount directive, a quantity in kilograms and two lines, parse cleanly. This is what the developers tested, and what a test suite built from examples shows.

### 2. Three ways to make inputs, 5000 runs each

Random printable text never gets past the first line: the parser wants the header "ORDER v2", so it covers one branch and finds no bug. Mutating the valid example files reaches 15 of the 16 branches and finds seven bugs. Keeping every input that reaches a new branch (the corpus grows from 2 to 10 inputs)
reaches all 16 and finds eight, including a loop that never ends, which needs "@repeat" followed by an odd number.

### 3. Each bug, as the smallest input that still triggers it

The findings are an array index error for a price like "2." (found on run 25), number format errors for prices and discounts that are not numbers, a string index error for "g" without "kg", a negative total from a negative price, a number format error for "@repeat" with nothing after it, and a hang after
"@repeat 1" on run 3,761. Each is shrunk by dropping chunks of the input while the same failure remains, until what is left is a few bytes. The first one is printed as the source of a unit test.

### 4. After the fix

The fixed parser checks every field with a strict pattern before using it, computes totals in long arithmetic and caps them, refuses negative numbers by pattern, ignores the unused @repeat directive, and reports every refusal as a ParseException. Each minimized input is now "refused" with a message or parsed without a loop.
Fuzzing the fixed parser the same way finds nothing in 5,000 runs. That is encouraging, and it is not a proof: fuzzing is a search. The regression inputs stay in the test suite, and the fuzzer runs again whenever the parser changes.

## The verdict

Fuzz every parser of untrusted data with a real coverage-guided fuzzer (Jazzer on the JVM), starting from valid example files and a dictionary of tokens, with a clear oracle. Keep the corpus, run it in CI for a short time and for longer on a schedule, and turn every finding into a regression test.

## How to recognise this in code you did not write

- `@FuzzTest` and `FuzzedDataProvider` in Jazzer tests.
- Corpus directories of small input files in a repository.
- OSS-Fuzz reports and crash reproducer files.
- `-XX:-OmitStackTraceInFastThrow` in fuzzing setups (hot exceptions lose their stack traces).

## Where you have already met this

- Crashes in image, PDF, archive and font libraries found by AFL and libFuzzer.
- OSS-Fuzz, which fuzzes open-source projects continuously.
- `@FuzzTest` in Jazzer, with JUnit 5.
- `ClassCastException` or `NegativeArraySizeException` reported in a bug tracker from a file that someone made.
