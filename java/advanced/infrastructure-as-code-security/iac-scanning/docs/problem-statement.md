# Problem Statement

## The scenario

An online store changes its Kubernetes manifests every day and wants mistakes caught before they are applied.

## The naive version

Run a scanner in the pipeline, read nothing, and always pass.

## What this project must deliver

- Rules written in code and tested with their own samples.
- Three real scanners compared on the same files.
- Exceptions that end.
- A baseline that fails the build on new findings only.
