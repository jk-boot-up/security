# Linux Hardening Checker, Explained

## The idea in one sentence

A host is hardened when its permissions, accounts, SSH, kernel options and listening ports match a written policy; machines can fix permissions safely, people decide the rest, and a scan in every build keeps it that way.

## The 4 acts

### 1. The shop's server image, before any fix

The input is the shop's server image. A file mode has four octal digits: the first holds the special bits (4 set-user-id, 2 set-group-id, 1 sticky), then one digit each for the owner, the group and everyone else, where read is 4, write 2 and execute 1.
/tmp/.hidden-shell has mode 4755: set-user-id, rwx for the owner, r-x for the group and others. It is not on the allow-list, so it is a copy of a shell that runs as its owner, root. /etc/shadow has mode 0644: everyone can read the password hashes, and the policy allows at most 0640.
The result is a report of 20 findings, worst first: 7 critical + 8 high + 3 medium + 2 low = 20. The 7 marked auto are file permissions that a machine can fix; the other 13 = 3 accounts + 4 sshd + 3 kernel + 3 ports need a person.

### 2. The fixes a machine can make

The input is the 20 findings, 7 of them marked auto. For each, the fixer either takes bits away or keeps only the bits the policy allows.
Taking away: the hidden shell 4755 - 4000 = 0755 (no set-user-id), the cron script 0775 - 0020 = 0755 (no group write), the upload folder 0777 - 0002 = 0775 and its file 0666 - 0002 = 0664 (no write for others).
Keeping only: /etc/shadow 0644 & 0640 = 0640, the SSH host key and the application config 0644 & 0600 = 0600.
The result: 7 changes, 20 - 7 = 13 findings left, none of them a permission. A second run makes 0 changes. The fixer does not touch accounts, services or settings: those are decisions.

### 3. The decisions a person makes

The input is the 13 findings left, and how the checker read them. In /etc/passwd the third field is the user id: toor:x:0:0 has UID 0, which is root's number, so toor is a second root. In /etc/shadow the second field is the password hash: test:: has it empty, so test logs in with no password.
The kernel's port table /proc/net/tcp writes each local address as hex: 00000000:0017 is address 0.0.0.0 (every interface) and port 0x0017 = 23. 0100007F:0CEA is the address 127.0.0.1 written backwards (little-endian) and port 0x0CEA = 3306: this host only. Ports 22 and 443 are allowed; 23, 2375 and 6379 are not.
Before the decisions the login accounts are root, toor, test and ana; PermitRootLogin, PasswordAuthentication, PermitEmptyPasswords and X11Forwarding are yes; randomize_va_space is 0, tcp_syncookies is unset, accept_redirects is 1; ports 22, 23, 443, 2375 and 6379 listen on every interface.
After them the login accounts are root and ana; the four SSH settings are no; the kernel settings are 2, 1 and 0; only 22 and 443 listen on every interface. The scan reports 0 findings.

### 4. Running it again, in every build

The input is a build rule, fail at HIGH or worse, and the hardened image: 0 findings, so the build passes.
Then a later change: a deployment script runs chmod 0777 on the upload folder, and an engineer adds a test account with a login shell.
The result: the same scan finds 2 findings, world-writable-dir on /var/www/uploads (auto) and default-account for test, both HIGH, so the build fails the day the regression appears.

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
