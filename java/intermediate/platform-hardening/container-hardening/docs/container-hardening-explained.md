# Container Hardening, Explained

## The idea in one sentence

Design a container for the day its process is taken over: non-root, no capabilities, no new privileges, read-only root, no host access, limits, secrets as files and an image pinned by digest, and know that this limits the damage and does not remove the flaw.

## The 5 acts

### 1. What 'docker run' with no options gives an attacker

The default container runs as root, has 14 capabilities including NET_RAW, a writable root file system and no limits. A hijacked process can read the secrets in its environment, change the application's files, drop and run a tool, sniff the pod network, and exhaust memory or processes.
The default seccomp profile blocks the dangerous system calls, and no host directories, sockets or namespaces are shared, so the kernel and the host are not directly in reach. Kubernetes classifies it as Baseline.

### 2. The configuration from the tutorial

The command that appears in many "how to run" pages is privileged, mounts the Docker socket and /etc, shares the host's network and process namespaces, and removes the seccomp filter. All thirteen attacks are possible, including starting a privileged container through the socket, which is root on the host.
The checker lists six reasons the container is not even Baseline.

### 3. One setting at a time

Starting from the tutorial configuration: removing privileged and the host namespaces and mounts cuts the attacks from 13 to 9; the default seccomp profile blocks the dangerous system calls (8); dropping all capabilities blocks raw sockets and mounting (6) and reaches Baseline. A non-root user alone changes nothing,
because set-user-id programs can still raise privileges; with no_new_privs the become-root attack is blocked (5) and the container reaches Restricted. A read-only root file system with a noexec temporary directory blocks tampering and dropping a tool (3), and memory and process limits block the last two (1).

### 4. The hardened container

The hardened container runs as user 10001, drops all capabilities, has a read-only root with a noexec tmpfs, forbids new privileges, has a 512 MB memory limit and a 200-process limit, takes its secrets from files, and uses an image pinned by digest. The same settings are printed as a docker run command and as a Kubernetes
securityContext, so what was checked is what is deployed. The level is Restricted. Of the thirteen attacks only one is left: a hijacked application can read the secrets it was given, because it needs them.

### 5. Settings that look harmless

Adding NET_RAW back keeps the level at Baseline (NET_RAW is in the default set, but Restricted wants every capability dropped) and still blocks sniffing, because the process is not root. Running as root with CAP_SYS_ADMIN, which looks like a small change, makes the level Privileged: mounting becomes possible, and root inside the container comes back.

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
