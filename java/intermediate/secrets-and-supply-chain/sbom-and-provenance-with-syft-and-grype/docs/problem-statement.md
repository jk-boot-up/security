# Problem Statement

## The scenario

An online store ships a container image that carries libraries it did not write, and one day a critical advisory is published for one of them.

## The naive version

Read the dependency file by hand, and find out from the news.

## What this project must deliver

- Real images with three versions of log4j.
- Bills of materials written by syft and scans by grype.
- A deployment gate with exceptions that end.
- The ways a scanner can be wrong.
