# Problem Statement

## The scenario

An online store is asked by a payment partner to show that it is secure enough for the data it holds. The team has fixed many bugs, but it cannot say which security requirements apply, which are met, and which are not, and it cannot keep that answer true from one release to the next.

## The naive version

Answer with a one-off penetration test and a promise to be careful. The test finds some bugs on the day it runs, and the answer is out of date by the next release.

## What this project must deliver

- A decision model that chooses the verification level and explains it.
- Requirements written down as data, in the team's words, with chapter, level and verification.
- Checks that ask the running store questions and answer pass or fail.
- A coverage report by chapter, and a release gate that refuses a regression.
- The same facts printed on every run.
