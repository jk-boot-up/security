# Passwords and multi-factor login: implementation plan

The projects are built in the order of the table in the module [README](../README.md), plain version first and then its tool version.

## Rules for building each project

1. Write the naive version and its failing demo first.
2. Add one control per act, and a test for every number the demo prints.
3. Generate `docs/expected-output.txt` from the demo, review it by eye, then commit it.
4. Write `concept.toml` from the numbers the demo printed, including the threat model and its diagram, then run `tools/conceptkit/conceptkit.sh build <project>`.
5. Review the video and the animation. Nothing is uploaded until the author asks.
