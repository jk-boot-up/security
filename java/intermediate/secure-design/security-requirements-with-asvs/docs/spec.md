# Spec — Security Requirements with OWASP ASVS

## Purpose

Teach how a team turns a verification standard into checked requirements: choosing a level, writing requirements as data, verifying them against a running application, and reporting coverage and regressions.

## Not in scope

- The standard's own text: the statements are paraphrases and the ids are the team's.
- The full standard: fourteen sample requirements.
- A compliance certification: the report is the team's own evidence.

## The problem

Teams cannot say which security requirements apply to them, which are met, and whether that is still true after the next release.

## The pieces

| Role | Class |
| --- | --- |
| Levels | `Level`, `LevelChooser`, `Profile` |
| Requirements as data | `Requirement`, `Catalogue`, `RequirementsFile` |
| The sample store | `SampleApp` |
| Checks | `Checks` |
| Coverage and the gate | `Assessment` |
| Entry point | `AsvsDemo` |

## Requirements

1. The level model gives L1, L2 and L3 for the three profiles, with reasons.
2. The requirements file round-trips, and a broken file is refused with its line number.
3. Every automated or configuration requirement names a known check, and every check answers pass or fail.
4. The coverage report counts met, failed, manual and not applicable, and a regression is reported as newly failing.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 48 lines, identical on every run |
| Acts | 5 |
| Video | 08:11, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
