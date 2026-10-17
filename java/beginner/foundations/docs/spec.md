# Foundations: spec

## The shared idea

Security controls are easier to learn when each one is tied to the harm it prevents. This module teaches that link
with small systems that fail first and are repaired second.

## What every project in this module must have

- A naive version that runs and fails visibly.
- Numbered acts, each printing exact results, stored in `docs/expected-output.txt` and compared by a test.
- A domain chosen to make the idea obvious; the domain is named in the project's README.
- A last act that shows the cost of the controls.
- Java 25 and Gradle, no framework, no network, no cloud account.

## The forced change

Each project ends by changing one thing the learner can try: a new role, a new limit, a new rule. The exercise names
the test that must change, so the cost of the change is visible.

## Scale limits

These projects teach intuition. They are not production designs: keys are demo values, storage is in memory and time is
simulated in ticks. Each README says what the simulation left out.
