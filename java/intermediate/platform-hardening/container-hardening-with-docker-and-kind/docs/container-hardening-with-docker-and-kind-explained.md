# Container Hardening with Docker and kind, Explained

## The idea in one sentence

Hardening a container is taking away, one option at a time, what a taken-over process could use, and measuring what is left.

## The 6 acts

### 1. What docker run with no options gives an attacker

The lab: the image shop-app:probe from alpine:3.24.3, with a user app (10001) that owns /app, a secret DB_PASSWORD in the environment, and thirteen shell attempts run inside the container with docker exec. The lab's Docker starts containers with no seccomp filter, so a profile that denies the 35 system calls Docker's own default profile denies is passed by file.
What the probes read: id -u is 0, CapEff is 00000000a80425fb, Seccomp is 2 (a filter is on), memory.max and pids.max are max. The capability mask is read bit by bit: bits 0, 1, 3, 4, 5, 6, 7, 8, 10, 13, 18, 27, 29 and 31 are set, Docker's fourteen default capabilities; (0xa80425fb >> 13) & 1 = 1, so NET_RAW is held, and (0xa80425fb >> 21) & 1 = 0, so SYS_ADMIN is not.
The result: all four attempts inside the container work, one in the kernel (raw sockets), none on the host, both on the node's resources: 4 + 1 + 0 + 2 = 7 of 13.

### 2. The configuration from the tutorial

The input is the command a tutorial shows to make things work: `--privileged`, `--security-opt seccomp=unconfined`, `--network host`, `--pid host`, `-v /var/run/docker.sock:/var/run/docker.sock` and `-v /etc:/hostetc:ro`.
The result: all thirteen attempts work. Seccomp is 0, so there is no filter; mount works; the Docker socket is there (control of every container and root on the host); /hostetc/passwd is readable; the container sees docker0 and veth interfaces and more than fifteen processes. 4 + 3 + 4 + 2 = 13 of 13.

### 3. One setting at a time

The input of each step is the container of the step before, with one option more. The result is the count, and what the step blocked and opened, worked out as the difference of the two lists of attempts that worked.
No host access and no privileged mode blocks mount-host and the four host attempts (13 to 8). A seccomp profile blocks dangerous-syscalls (7). Dropping all capabilities blocks sniff-network and, less obviously, tamper-with-app: root without DAC_OVERRIDE cannot write files that belong to the app user (5).
A user that is not root blocks become-root but opens tamper-with-app again, because the app user owns /app (5). No new privileges moves nothing, because this image has no set-user-id program; in an image that has one, it is what stops it. A read-only root with a noexec /tmp blocks tamper-with-app and drop-and-run (3), and limits block exhaust-memory and fork-bomb (1).

### 4. The hardened container

The input is every option of the steps together. What the probes read shows the kernel enforcing each one: id -u is 10001, the capability mask is 0000000000000000, Seccomp is 2, memory.max is 536870912 bytes (536870912 / 1048576 = 512 MiB) and pids.max is 200.
The result: twelve attempts are blocked, each with what the probe saw; one works, read-secrets, because the application must read its secret, so a hijacked application can too. 1 + 0 + 0 + 0 = 1 of 13.

### 5. Settings that look harmless

The input is the hardened container with one small change. The result is the new count and the attempts that work again, against the hardened container.
Adding NET_RAW opens nothing: the process is not root. Running as root with SYS_ADMIN opens become-root; mount-host is still blocked, but only because the seccomp profile denies mount. Removing the filter as well opens dangerous-syscalls and mount-host: 4 of 13. Each layer covers for a mistake in the others.

### 6. The same on Kubernetes, with Pod Security Standards on kind

The lab is a real one-node kind cluster, with three namespaces labelled to enforce the privileged, baseline and restricted Pod Security Standards. Three pods, the tutorial's, a plain one and the hardened one, are sent with `kubectl apply --dry-run=server`, so the admission decides and nothing is created.
The result is a table of admitted and refused, with how many checks a refusal lists. Baseline refuses the tutorial's pod for host namespaces, hostPath volumes and privileged. Restricted refuses the plain pod for allowPrivilegeEscalation != false, unrestricted capabilities, runAsNonRoot != true and seccompProfile. A namespace that only warns creates the plain pod and prints the same 4 checks as a warning.
Probed from inside, the plain pod admitted by baseline still allows 7 attempts (it has no seccomp filter at all), and the hardened pod admitted by restricted 2: read-secrets, and drop-and-run, because its /tmp is a writable emptyDir that can run a script. The standards judge the spec once, when the pod is created.

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
