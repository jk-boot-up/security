# IaC Scanning, Explained

## The idea in one sentence

A scanner is only as good as its rules, its exceptions and the discipline of acting on what it says.

## The 5 acts

### 1. Eight rules in Java, on the store's manifests

The input is what each container sets in the first manifests: web runs shop/web:latest with privileged=true; orders runs shop/orders with no tag and DB_PASSWORD=hunter2 written in the file; the agent runs shop/metrics-agent:2.4.1 with hostNetwork=true, limits, runAsNonRoot, a read-only root filesystem and no privilege escalation. Each of the eight rules is a question about one container.
The result is a matrix of containers by rules: web breaks K1, K2, K3, K4, K7 and K8 (6); orders K2, K3, K4, K5, K7 and K8 (6); the agent K6 (1): 6 + 6 + 1 = 13. A setting that is missing counts as its default, which is why web breaks six rules while it sets only two things. After the fixes the matrix keeps one mark: K6 on DaemonSet/metrics-agent.

### 2. Testing the rules with their own samples

The input is sixteen samples, two per rule, each a snippet put into a small Pod: for example K1 bad securityContext: {privileged: true} and good {privileged: false}; K3 bad image: shop/orders and good image: shop/web:1.8.2. A bad sample such as imagePullPolicy: Always sets nothing the rule asks about, so the default applies and the rule must fire.
The result: 8 of 8 rules fire on bad and stay quiet on good. Then a rule with a mistake: K3 written as "the image ends with :latest". On its bad sample shop/orders it does not fire, so its own test fails. How the real K3 reads shop/orders: the last part after the last / is orders; it has no : or @, so the tag is none and Kubernetes pulls latest; K3 fires.

### 3. Three scanners on the same files

The input is the same two folders of manifests, read by three tools at pinned versions, so the checks do not change with a download. For each of our rules, the table shows the first check each tool fired on the first files: K1 is CKV_K8S_16, KSV-0017 and Container Is Privileged, and so on.
The comparison: 7 of 8 rules are found by all three, and K5, a secret written in the manifest, by none. The password two ways: written in the file, no tool flags it; read from a Secret in an environment variable, Checkov (CKV_K8S_35) and KICS (Secrets As Environment Variables) flag the better practice, because they prefer secrets mounted as files. On the fixed files Checkov still fails 12 checks, Trivy 8 and KICS 14: advice that goes beyond our eight rules.

### 4. Exceptions: a waiver with an owner and an end

The input is the one finding left, K6 on DaemonSet/metrics-agent, and a waiver for it: owner platform team, reason reads node counters from the host network, ends 2026-12-31. The dates are fixed, so the output does not depend on the day it runs.
The result by day and waiver: on 2026-10-22 the waiver leaves 0 findings; on 2027-01-01 it has ended, so 1 finding is back and the waiver is reported as stale; a waiver with no owner leaves 1; a waiver for DaemonSet/other leaves 1. Inside the tools: a checkov.io/skip1 annotation for CKV_K8S_19 and a .trivyignore line KSV-0009 turn each check from failing (yes) to passing (no). Neither of those has an end date.

### 5. A baseline in the build

The input is the baseline, the findings accepted so far (K6 on DaemonSet/metrics-agent), and a change: a new Deployment/cart that runs shop/cart:latest and sets nothing else. The scan now gives 6 findings.
How the build decides: 6 now - 1 accepted = 5 new, K2, K3, K4, K7 and K8 on cart, so the build fails, while the accepted finding blocks no one; 0 accepted findings were fixed. What is left for people: no tool flags a password written in the file, two flag the good practice of a Secret, and a waiver needs an owner and an end date.

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
