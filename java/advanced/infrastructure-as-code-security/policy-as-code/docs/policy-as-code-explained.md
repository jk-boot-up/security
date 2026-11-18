# Policy as Code, Explained

## The idea in one sentence

A policy that cannot say no to what it did not expect is not a policy.

## The 4 acts

### 1. Deny by default

The rules in policy/main.rego are twelve deny rules, each with a comment that names it: R1 an allowed kind, R2 the bank's registry, R3 runAsNonRoot is true, R4 not privileged, R5 a cpu limit and a memory limit, R6 an owner label, R7 no host network, R8 no latest tag, R9 not the default namespace, R1 a kind at all, and R10 at least one container.
The input "three mistakes" is the good request with the image docker.io/library/nginx:latest and no securityContext. The result is the set of messages of data.main.deny: R2 (a foreign registry), R3 (may run as root) and R8 (the latest tag). Allow is true only when there are no messages, so allow is false.
For each of the 13 requests a reviewer wrote down the rules it breaks. The policy's answers are the same for all 13.

### 2. The holes: a missing field

Before the fix, the draft has ten deny rules. It allows the empty object (the reviewer expects R1) and the Deployment with no containers (the reviewer expects R10), so it agrees on 11 of 13.
How the empty object is read: object.kind is undefined; the line not object.kind in allowed_kinds is undefined too, so the rule body fails and no message is made. The line not object.kind is true. How the Deployment with no containers is read: count(containers) is 0, so every rule that loops over the containers has nothing to check, and count(containers) == 0 is true.
After the fix, two rules ask for what is required: R1 a kind at all (not object.kind) and R10 at least one container (count(containers) == 0). The policy agrees on 13 of 13.
The same trap in a list of what is forbidden: for the good request with no securityContext and no limits, c.securityContext.runAsNonRoot is undefined, so naive.rego's runAsNonRoot == false is undefined and it allows the container. main.rego's not runAsNonRoot == true is true, so it denies with R3 and R5.

### 3. Testing the policy and its tests

The input is policy/main_test.rego: opa test runs 17 tests and all pass.
How a mutant is made: rule 5 of 12 (R5: a cpu limit) starts with deny contains msg if; the mutant renames it to deny_off contains msg if. Nobody asks for deny_off, so the rule is switched off. If no test fails, no test checks that rule.
The result, per rule: 3 tests (the good request and the two R1 tests) notice 2 mutants, the two R1 rules. 13 tests, one for each rule, notice 8: they miss the cpu limit alone, the memory limit alone, a kind at all, and at least one container. All 17 tests notice all 12.

### 4. In the cluster and in the build

The input in the cluster is an AdmissionReview (apiVersion admission.k8s.io/v1) with uid 705ab4f5 and a Deployment named payments whose image is docker.io/library/nginx:latest. The result is the response: allowed false, with the reasons R2 (a foreign registry) and R8 (the latest tag). The good Deployment gets allowed true.
The input in the build is three files: a-payments.yaml with registry.bank.example/payments:2.3.1, b-cart.yaml with registry.bank.example/payments:latest, and c-legacy.yaml with docker.io/library/nginx:latest. Conftest passes the first, fails b-cart.yaml on R8 and c-legacy.yaml on R2 and R8, and exits 1, so the build stops.
A new rule, R11, asks for a read-only root filesystem, which payments.yaml does not set. First as a warning (warn contains msg): exit 0, 1 warning, 0 failures, the build goes on. Later as a denial (deny contains msg): exit 1, 0 warnings, 1 failure, the build stops.

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
