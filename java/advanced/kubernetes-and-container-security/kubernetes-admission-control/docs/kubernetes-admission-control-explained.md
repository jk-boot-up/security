# Kubernetes Admission Control, Explained

## The idea in one sentence

Admission control is the last gate before a workload exists, and a gate that is down is no gate.

## The 5 acts

### 1. Policies that refuse

A privileged pod, a pod with no limits, an image from another registry, an image nobody signed and a privileged Deployment are refused. A pod that breaks three rules is refused by all of them. A signed image is admitted, by digest, by tag or tagged latest. The same privileged pod in a namespace without the label is admitted: admission covers only what is labelled.

### 2. Audit first, then deny

The legacy agent is privileged and keeps running when the policies arrive, because admission sees requests, not what already runs. A rule in audit mode admits a pod with a writable root filesystem and the policy report says fail for it and for the legacy agent. When the rule is switched to deny the next such pod is refused.

### 3. Mutation and signed images

A mutating policy adds allowPrivilegeEscalation false to containers that did not choose and keeps the settings they did choose. The image policy resolves the tag :1 of the signed image to its digest and writes the digest into the pod, so what runs is what was signed. A privileged pod is still refused after the mutation.

### 4. Exceptions

node-agent is admitted while the exception lasts. Another pod with the same spec is refused. The same exception written in the team's own namespace is ignored, and an exception that ended in 2020 is ignored too. One request of four is admitted.

### 5. The bill

A stopped Kyverno removes its webhooks, and with none left the API server admits everything. Admission is also blind to what already runs and to namespaces without the label. The cost is an engine in the path of every request that must stay available.

## The verdict

Refuse by default in labelled namespaces, start every new rule in audit mode, sign what you deploy and deploy by digest, keep exceptions where teams cannot write, and keep the engine available.

## How to recognise this in code you did not write

- A cluster where anyone can run a privileged pod.
- Images pulled by tag from any registry.
- A policy engine installed with failurePolicy Ignore and never tested.
- Exceptions written in the team's own namespace.

## Where you have already met this

- A cluster where every developer is an administrator.
- An image pulled by tag from any registry.
- A policy engine installed in audit mode and left there.
- An exception granted in a chat message.
