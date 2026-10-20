# Linux Hardening Checker, Explained

## The idea in one sentence

A host is hardened when its permissions, accounts, SSH, kernel options and listening ports match a written policy; machines can fix permissions safely, people decide the rest, and a scan in every build keeps it that way.

## The 4 acts

### 1. What the checker finds on a typical image

The checker reads the image and reports, worst first: a login with no password, a second UID 0 account called toor, telnet, the Docker API and Redis listening on every interface, empty passwords allowed in SSH, and a copy of a shell with the set-user-id bit in /tmp (critical).
The high findings include a group-writable cron script, a test account with a shell, a shadow file, an SSH host key and an application config with a password that are readable by everyone, root login over SSH, and a world-writable upload directory. Seven of the twenty are permission problems that a machine can fix.

### 2. The fixes a machine can make

The checker removes the set-user-id bit from the unexpected file, takes world write away from the upload directory and its file, group write away from the cron script, and tightens shadow, the SSH host key and the application config to the modes the policy names.
It does not touch accounts, services or settings. The second scan still reports thirteen findings, all of them decisions: the empty password and the extra root account, the three ports, the SSH settings, the kernel options and the default account.

### 3. The decisions a person makes

A person reads the thirteen findings and decides: delete the toor and test accounts, set the SSH daemon to refuse root login, passwords and empty passwords, set the three kernel options, and stop the telnet, Docker API and Redis services (the port table now shows only SSH, the local-only service and HTTPS).
The scan is clean. The result is a baseline: the same scan, with the same policy, goes into the image build.

### 4. Running it again

Hardening decays: a deployment script makes a directory world-writable again, an engineer adds a test account to debug something. The scan is cheap, so it runs in the image build and on running hosts; a new finding fails the build or raises an alert.

## The verdict

Write the policy, scan the image in the build and the hosts regularly, automate only the safe, reviewed fixes, treat every new finding as a regression, and use maintained benchmark tools for the full list.

## How to recognise this in code you did not write

- `find / -perm -4000 -type f` in an incident report.
- `PermitRootLogin`, `PasswordAuthentication` and `PermitEmptyPasswords` in `sshd_config`.
- `chmod 600 id_rsa`.
- Lynis, OpenSCAP and CIS-CAT output.

## Where you have already met this

- `find / -perm -4000` in privilege escalation guides.
- `PermitRootLogin no` and `PasswordAuthentication no` in every SSH hardening list.
- Lynis, OpenSCAP and CIS-CAT reports.
- `chmod 600` on private keys, and "unprotected private key file" errors from ssh.
