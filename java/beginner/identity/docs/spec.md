# Identity: spec

## The shared idea

Each project teaches one concept with a small system that fails first and is repaired second, in an online store or an online bank, whichever fits the topic better.

## What every project in this module must have

- A naive version that runs and fails visibly.
- Numbered acts, each printing exact results, stored in `docs/expected-output.txt` and compared by a test.
- A last act that shows what the control costs or leaves open.
- Java 25 and Gradle. A plain project uses only the JDK. A project with a tool in its name uses that tool, and keeps its default test task free of anything that needs Docker or a program to be installed.
- A threat model with a diagram, a STRIDE table, assumptions and an out-of-scope list.
- Values from published standards checked in tests wherever a standard has test values.

## Scale limits

These projects teach intuition. They are not production designs: keys and secrets that appear in the code are demo values, storage is in memory and time is simulated in ticks or seconds. Each README says what the simulation left out.
