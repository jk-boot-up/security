# Container Hardening with Docker and kind, Explained

## The idea in one sentence

Hardening a container is taking away, one option at a time, what a taken-over process could use, and measuring what is left.

## The 5 acts

### 1. What docker run with no options gives an attacker

Thirteen attempts run inside a real container as the user it runs as. With no options (and a seccomp profile passed by file, because the lab's Docker has none of its own), seven work: the secrets in the environment, the writable root file system, a file written and run, root, a raw socket through NET_RAW, no memory limit and no process limit. Six are blocked: dangerous system calls (the profile), mount, the Docker socket, host files, the host network and the host's processes.

### 2. The configuration from the tutorial

docker run --privileged --security-opt seccomp=unconfined --network host --pid host -v /var/run/docker.sock:/var/run/docker.sock -v /etc:/hostetc:ro shop-app. All 13 of 13 attacks are possible: blocked 13 - 13 = 0. One command that works in a tutorial gives the process the host.

### 3. One setting at a time

Each line adds one option to the one above: the tutorial's 13; no privileged, no host network, PID or mounts, 8; a seccomp profile, 7; drop all capabilities, 5; run as a user that is not root, 5 (a different set: root is gone, and the files the app user owns are open); no new privileges, 5; read-only root and a noexec temporary directory, 3; memory and process limits, 1. No new privileges does not move the number here, because this image has no set-user-id program for an attacker to use.

### 4. The hardened container

docker run --user 10001 --cap-drop=ALL --read-only --tmpfs /tmp:rw,noexec,nosuid --security-opt no-new-privileges --security-opt seccomp=<profile> --memory 512m --pids-limit 200. One of 13 attacks is possible: read-secrets; the application must read its secrets, so a hijacked application can too. Adding NET_RAW, which looks harmless, changes nothing, because it matters only to root. Adding root and SYS_ADMIN brings back root, and only the seccomp profile still stands between it and mount. Without the filter as well, four are possible: each layer covers for a mistake in the others.

### 5. The same on Kubernetes, with Pod Security Standards on kind

A one-node kind cluster with three namespaces that enforce the privileged, baseline and restricted standards, and three pods sent with --dry-run=server so that the admission decides. The tutorial's pod is admitted by privileged, refused by baseline (host namespaces, hostPath volumes, privileged) and by restricted (7 violations). A plain pod is admitted by baseline and refused by restricted (allowPrivilegeEscalation, unrestricted capabilities, runAsNonRoot, seccompProfile). The hardened pod is admitted everywhere. A namespace that only warns creates the plain pod with a warning that lists 4 violations. Probed from inside, the plain pod has 7 attacks possible and the hardened pod 2: the secret, and a writable temporary directory that still runs code.

## The verdict

Run as a user that is not root, drop all capabilities, keep the root file system read-only with a noexec temporary directory, filter system calls, set memory and process limits, never mount the Docker socket or host paths, let the cluster refuse what you did not mean to run, and keep secrets as short-lived as you can.

## How to recognise this in code you did not write

- `--privileged` or `--cap-add` in a command or a compose file.
- `securityContext` in a manifest.
- `pod-security.kubernetes.io/enforce` on a namespace.
- A `kind create cluster` in a build script.

## Where you have already met this

- `docker run --privileged` in a tutorial.
- `securityContext` in a manifest.
- A namespace label `pod-security.kubernetes.io/enforce`.
- A "permission denied" after someone added `readOnlyRootFilesystem`.
