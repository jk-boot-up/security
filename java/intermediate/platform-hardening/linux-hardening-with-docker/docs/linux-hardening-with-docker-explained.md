# Linux Hardening with Docker, Explained

## The idea in one sentence

An image is a recipe, and a container is a copy of what it makes: a setting that is only fixed in the copy is lost with the copy.

## The 5 acts

### 1. What the scan finds in a typical image

shop-host:typical is a real container from a real image, scanned by one script run as root inside it: files and modes, accounts, sshd_config, listeners from /proc/net/tcp, kernel settings. It finds 19 findings: 7 critical (an account with no password, a second root, a set-user-id copy of a shell, empty passwords in sshd_config, and three listeners: telnet, an unauthenticated Docker API and Redis), 8 high, 2 medium and 2 low. Seven can be fixed mechanically.

### 2. The fixes a machine can make, and where they live

Seven permission changes applied in the running container leave 12 findings, none of them a permission. docker diff lists what changed in the container's own writable layer: the cron script, the shadow file, the host key, the config, the copied shell and the upload folder. A new container from the same image has 19 findings again: a fix in a running container is not a fix. The image is what runs next time, so a change belongs in the image's recipe.

### 3. The decisions a person makes, in the recipe

The decisions go in the recipe: no second root account and no test account; a locked service account with no login shell; sshd_config with root login, passwords, empty passwords and X11 off; private modes for the key, the config and cron; no copied shell with the set-user-id bit; one listener, port 8080; the process runs as app, not root; and the namespaced kernel settings the container is started with: accept_redirects 0 and syncookies 1. shop-host:hardened, scanned the same way, has 0 findings. The typical image's process runs as root; the hardened one as app.

### 4. Running it again

Someone loosens the config file inside a running container: the scan finds loose-permissions and world-writable-file for /opt/app/config.properties. A new container from the image has 0 findings. The scan is part of the build, on the image after it is built, and a regression fails it; on running containers it finds drift, and the answer to drift is a new container.

### 5. The bill

kernel.randomize_va_space inside the container is the host kernel's value, 2; writing 0 to it from inside is refused: a read-only file system. A container shares the host's kernel, so these checks are the host's to fix. The scan runs as root inside the container; a process that runs as app sees less, and an attacker starts as that process. The allow-lists, port 8080 and no set-user-id file, are the shop's own decisions. How a container is started, with which user, capabilities, mounts and filesystem mode, is the subject of container-hardening.

## The verdict

Scan the image in the build, make every decision in the recipe, run as a service account, start containers with safe kernel settings, treat drift as a reason for a new container, and remember that the kernel belongs to the host.

## How to recognise this in code you did not write

- `USER app` in a Dockerfile.
- `docker diff` output.
- `chmod` in a Dockerfile.
- A benchmark report with CIS numbers.

## Where you have already met this

- A `USER` line, or its absence, in a Dockerfile.
- `chmod 777` in a deployment script to "make it work".
- A CIS benchmark report.
- `docker exec` used to fix something in production.
