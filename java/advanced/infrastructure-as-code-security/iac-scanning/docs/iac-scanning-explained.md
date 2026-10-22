# IaC Scanning, Explained

## The idea in one sentence

A scanner is only as good as its rules, its exceptions and the discipline of acting on what it says.

## The 5 acts

### 1. Eight rules in Java

Three workloads: web with a privileged container and a latest tag, orders with a password written in the env block, and a metrics agent that needs the host network. The rules report 13 findings; after the fixes (a pinned image, limits, a read-only filesystem, no privilege escalation, the password from a Secret) one remains: the agent's host network.

### 2. Testing the rules

A rule is code, so it gets tests: a sample that must fire it and one that must not. All eight pass. A K3 that only looks for :latest is caught by the sample with no tag: it does not fire on its bad sample.

### 3. Three scanners on the same files

Checkov, Trivy and KICS read the same manifests. For seven of the eight rules each has a check. For the password written in the env block none does, while Checkov and KICS flag the fixed version, which reads it from a Secret. All three still give advice on the fixed files.

### 4. Exceptions

The metrics agent needs the host network. A waiver naming an owner, a reason and 2026-12-31 hides the finding until that day; on 2027-01-01 the finding is back and the waiver is stale. A waiver with no owner hides nothing, and neither does one for another resource. Checkov's annotation and Trivy's ignore file hide the same finding inside the tools.

### 5. A baseline, and the bill

Adding a cart service with a latest tag gives 5 new findings against the accepted list, and none fixed. The old findings do not block anyone, and the list can only shrink. The bill: noise, blind spots, exceptions that do not end, and files that say nothing about the running cluster.

## The verdict

Test your own rules, use more than one scanner and know what each misses, give every exception an owner and an end date, and let a baseline stop only the new problems.

## How to recognise this in code you did not write

- `checkov.io/skip1` annotations with no reason.
- A `.trivyignore` file that only grows.
- A pipeline step with `|| true`.
- `image: x:latest`.

## Where you have already met this

- A CI job that prints 300 warnings and always passes.
- A `latest` tag in a Deployment.
- A comment saying `# TODO remove` on an exception.
- A password in an env block.
