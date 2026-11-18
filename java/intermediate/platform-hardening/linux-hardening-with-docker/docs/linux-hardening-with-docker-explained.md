# Linux Hardening with Docker, Explained

## The idea in one sentence

An image is a recipe, and a container is a copy of what it makes: a setting that is only fixed in the copy is lost with the copy.

## The 5 acts

### 1. What the scan finds in a typical image

The input is a real container from the image shop-host:typical, read by one script run as root inside it. It finds two UID 0 accounts (root and toor), an account with an empty password (test), a set-user-id copy of a shell at /tmp/.hidden-shell, a world-writable upload folder (777) and file (666), modes 644 on /etc/shadow, the SSH host key and the application config, 775 on the cron script, PermitRootLogin, PasswordAuthentication, PermitEmptyPasswords and X11Forwarding all yes, accept_redirects 1 and tcp_syncookies 0.
The listeners come from /proc/net/tcp6 in hex: the address of all zeros is ::, every interface, and the ports are 0x0017 = 23 (telnet), 0x0947 = 2375 (the Docker API) and 0x18EB = 6379 (Redis), none of them allowed.
The result is 19 findings: 7 critical + 8 high + 2 medium + 2 low. The 7 marked auto are file permissions with a fix command; the other 12 = 3 accounts + 4 sshd + 2 kernel + 3 ports need a person.

### 2. The fixes a machine can make, and where they live

The input is the 7 auto findings and their fix commands: chmod u-s on the hidden shell, chmod g-w on the cron script, chmod 640 on /etc/shadow, chmod 600 on the host key and the config, chmod o-w on the upload folder and its file. They run as root in the running container.
The result in that container: 19 - 7 = 12 findings, none of them a permission. docker diff shows where the changes live: the seven files, marked C for changed, in the container's own writable layer, and 8 parent folders.
A new container from the same image has 19 findings again: the fixes stayed in the old container's layer. The image is what runs next time, so a change belongs in the image's recipe.

### 3. The decisions a person makes, in the recipe

The input is the hardened recipe, shop-host:hardened: no toor and no test account; a service account app with the shell /sbin/nologin; modes 600 for the host key and the config and 750 for cron; no copied shell; sshd_config with the four settings set to no; one listener on port 8080; USER app; and the container started with --sysctl accept_redirects=0 and tcp_syncookies=1.
The result, read by the same script: UID 0 is only root, no empty password, no set-user-id file, nothing world-writable, modes 640, 600, 600 and 750, the four sshd settings no, accept_redirects 0, tcp_syncookies 1, and one listener, :: port 8080. The scan has 0 findings. The image's process runs as root in the typical image and as app in the hardened one; id -un inside the hardened container says app.

### 4. Running it again

The input is a running hardened container with 0 findings, and someone who runs chmod 666 /opt/app/config.properties inside it.
The result: the same scan finds loose-permissions and world-writable-file on the config, both HIGH. The bits that are too many are worked out as 0666 & ~0600 = 0066: read and write for the group and for everyone else.
A new container from the image has 0 findings. The scan runs in the build on the image, and on running containers it finds drift; the answer to drift is a new container.

### 5. The bill

The input is the kernel, seen from inside a container: kernel.randomize_va_space reads 2, which is the host's value. Writing 0 to it from inside is refused: a read-only file system.
What it means: a container shares the host's kernel, so kernel settings are the host's to fix. The scan ran as root; an attacker starts as the app process and sees less. The allow-lists, port 8080 and no set-user-id file, are the shop's own decisions. How a container is started, with which user, capabilities and mounts, is the subject of container-hardening.

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
