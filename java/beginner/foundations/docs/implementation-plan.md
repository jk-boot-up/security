# Foundations: implementation plan

1. **cia-triad** (built): the three promises, an online store, five acts.
2. **threats-and-risk**: list a shop's endpoints as an attack surface, score each threat by likelihood and impact, and
   rank the risks.

## Rules for building each project

1. Write the naive version and its failing demo first.
2. Add one control per act, and a test for every number the demo prints.
3. Generate `docs/expected-output.txt` from the demo, review it by eye, then commit it.
4. Write `concept.toml` from the numbers the demo printed, then run `tools/conceptkit/conceptkit.sh build <project>`.
5. Review the video and the animation. Nothing is uploaded until the author asks.
