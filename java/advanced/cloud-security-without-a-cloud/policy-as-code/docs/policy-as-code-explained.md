# Policy as Code, Explained

## The idea in one sentence

A policy that cannot say no to what it did not expect is not a policy.

## The 5 acts

### 1. Deny by default

Each request has the rules a reviewer expects it to break: a foreign registry, a latest tag, no security context, privileged, no limits, no owner label, host network, a ClusterRoleBinding, the default namespace, three mistakes at once, an empty object and a Deployment with no containers. The good request is allowed; the others are denied with the right rule numbers. The policy agrees on 13 of 13.

### 2. The holes

Without the two guard rules the policy allows an empty object and a Deployment with no containers. The same trap in a list of what is forbidden: the rule compares a field to false, the field is missing, the comparison is undefined and the container passes. The policy that asks for what is required denies the container with no security context and no limits: R3 and R5.

### 3. Testing the policy and its tests

opa test runs the Rego tests: 17 of 17. To test the tests, each of the 12 deny rules is switched off in turn and the suite is run again. Three tests notice 2 mutants, one test for each rule notices 8, and the full suite notices 12. The four it misses are the cases a happy-path test never reaches: only the cpu limit missing, only the memory limit, no kind, no containers.

### 4. In the cluster and in the build

The same file reads an AdmissionReview and plain manifests. A Deployment from docker.io with a latest tag is not allowed, with 2 reasons; the good one is. Conftest on three files exits 1 and lists the failures by file. A new rule for a writable root filesystem is a warning first (exit 0, 1 warning) and a denial after (exit 1).

### 5. The bill

A policy is reviewed, versioned and tested, and its tests are tested. A missing field must mean deny. Roll a rule out as a warning before it denies. The cost is a language to learn, a second program of tests, and an engine in the request path.

## The verdict

Ask for what is required, deny when a field is missing, test every rule with a case it must deny and one it must allow, test the tests by switching rules off, and roll out a new rule as a warning.

## How to recognise this in code you did not write

- `deny` rules that compare a field with `false`.
- `opa test` with only happy-path cases.
- A policy repository with no tests.
- A webhook with `failurePolicy: Ignore`.

## Where you have already met this

- A wiki page of deployment rules that nobody checks.
- A pipeline step that greps for `latest`.
- A policy that was copied and never tested.
- An exception made in the cluster by hand.
