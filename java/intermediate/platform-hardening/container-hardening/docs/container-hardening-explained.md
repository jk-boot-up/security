# Container Hardening, Explained

## The idea in one sentence

Design a container for the day its process is taken over: non-root, no capabilities, no new privileges, read-only root, no host access, limits, secrets as files and an image pinned by digest, and know that this limits the damage and does not remove the flaw.

## The 5 acts

### 1. What 'docker run' with no options gives an attacker

The input is what `docker run shop-app:latest` gives when nobody adds a flag: the user is root, the container has Docker's 14 default capabilities (NET_RAW among them), the root file system is writable, set-user-id programs may raise privileges, the default seccomp profile is on, nothing of the host is shared, there are no memory or process limits, and the secrets are environment variables.
The result, by what each attack reaches: inside the container all 4 are possible (read the secrets, change the app, drop and run a tool, be root); in the kernel only sniffing with NET_RAW (the seccomp profile blocks the dangerous calls, and there is no SYS_ADMIN); nothing on the host; and both resource attacks. 4 + 1 + 0 + 2 = 7 of 13.
The level is worked out check by check: all six Baseline checks pass, and the three Restricted checks (not root, no privilege escalation, drops ALL capabilities) fail, so the level is Baseline.

### 2. The configuration from the tutorial

The input is the command many "how to run" pages show: `--privileged`, `--security-opt seccomp=unconfined`, `--network host`, `--pid host`, `-v /var/run/docker.sock:/var/run/docker.sock` and `-v /etc:/etc`. The settings it gives: root, every capability, no seccomp filter, the host's network and processes and two host mounts.
The result: every attack in every group, 4 + 3 + 4 + 2 = 13 of 13. Six Baseline checks fail (privileged, host network, host PID namespace, host path volumes, seccomp unconfined, a capability beyond the default set: ALL), so the level is Privileged.
The Docker socket is the worst of them: whoever can write to it can ask the Docker daemon, which runs as root, to start a privileged container with the host's / mounted, which is a root shell on the host.

### 3. One setting at a time

The input of each step is the container of the step before, with one setting more. The result is the count, the level, and the attacks that step blocked, worked out as the difference of the two lists.
No host access blocks docker-socket, host-files, host-network and see-host-processes (13 to 9). The default seccomp profile blocks dangerous-syscalls (8). Dropping all capabilities blocks sniff-network and mount-host (6) and passes Baseline.
A user that is not root blocks nothing on its own, because set-user-id programs in the image can still raise privileges. No new privileges sets no_new_privs and blocks become-root (5); with it, all three Restricted checks pass. A read-only root with a noexec /tmp blocks tamper-with-app and drop-and-run (3), and limits block exhaust-memory and fork-bomb (1).

### 4. The hardened container

The input is the hardened container: user 10001, all capabilities dropped, a read-only root with a noexec, nosuid tmpfs on /tmp, no new privileges, 512 MB of memory and 200 processes at most, secrets as files, and an image pinned by its digest.
The same settings are written twice from one object, as a `docker run` command and as a Kubernetes `securityContext`, so what was checked is what is deployed.
The result, check by check: all six Baseline checks and all three Restricted checks pass, so the level is Restricted. One attack is still possible, read-secrets: the application must read its secrets, so a hijacked application can read them too. Hardening limits the damage; it does not fix the flaw.

### 5. Settings that look harmless

The input is the hardened container with one small change. The result is the new count and level, the attacks that came back, and the checks that now fail.
Adding NET_RAW, which is in Docker's default set, opens no attack: sniff-network stays blocked because the process is not root. But the Restricted check "drops ALL capabilities" fails, so the level falls to Baseline.
Running as root with SYS_ADMIN looks like one more small change. It opens become-root and mount-host (3 of 13), and fails three checks: only default capabilities (Baseline), not root and drops ALL capabilities (Restricted), so the level is Privileged.

## The verdict

Aim for the Restricted Pod Security level for application workloads. Use distroless or minimal non-root images, pin by digest, pass secrets as files from a secret store, add network policies, and enforce the standards with admission control so that a new workload cannot drift.

## How to recognise this in code you did not write

- `--privileged`, `--cap-add`, `-v /var/run/docker.sock`, `--network host`, `--pid host`.
- `securityContext`, `runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`.
- `pod-security.kubernetes.io/enforce: restricted` on a namespace.
- CIS Docker Benchmark findings.

## Where you have already met this

- `docker run --privileged` and `-v /var/run/docker.sock:/var/run/docker.sock` in tutorials.
- `securityContext` in Kubernetes manifests, and Pod Security admission labels.
- Distroless and non-root base images.
- A scanner report titled "container runs as root".
