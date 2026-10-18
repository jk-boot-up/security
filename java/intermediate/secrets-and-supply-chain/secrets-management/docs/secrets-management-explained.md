# Secrets Management, Explained

## The idea in one sentence

Secrets should never live in code, images or environment variables; they should be injected at run time, never logged, and ideally leased per
application for a short time from a secrets service that can rotate and revoke them, which then becomes the thing to protect.

## The 5 acts

### 1. Where secrets leak

The scanner reads the three files the team committed and finds three secrets: a database password and a payment key named in a
properties file, and a random-looking value in a script. The production file names where its secrets come from and holds none. A key in an
environment variable is read by any program the store starts, and by crash reports and debug pages that print the environment. And an image
built with the key, which a later layer deletes, hides it from the running container, but anyone with the image file reads it from layer two.

### 2. Secrets injected at run time

Secrets are injected when the store starts, as files in a mounted folder, the way container platforms do. A secret file with permissions
rw-r--r-- is refused at startup, because others can read it. The same file with rw------- is read once: twenty-four characters, nothing in
the code or the image. Written into a log line, the secret shows as [redacted], because the Secret type never prints its value. Once the
connection pool has its copy, the startup copy is wiped, and using it fails.

### 3. A secrets service that hands out leased logins

Instead of one shared password, a secrets service hands out logins. The shop API proves who it is and gets a database login of its own,
v-shop-api-1, for 900 seconds. The reports job gets a different login; no two applications share one. A wrong application credential is
refused. At t=600 the shop's login works, and renewing extends it to t=1500. The reports lease, never renewed, ends at t=900 and its login is
removed from the database. Renewals stop at the maximum of 3600 seconds after issue, and at t=3600 the database refuses the shop's login, so
the shop asks for a new one. The audit trail records every request: ten entries.

### 4. Rotation without downtime, and the blast radius of a leak

The payment provider issues key v2 while v1 is still in use, and during the changeover it accepts both. Every server of the store switches to
v2, then v1 is revoked: v1 is refused and v2 works, with no moment in which neither did. Then the blast radius of a leak. A leased login copied
at t=600 works for at most 300 more seconds, or 3000 if the thief can renew it, and once the leak is noticed, revoking the lease ends it at once.
A static database password copied at the same moment still works three years later, until someone remembers to change it.

### 5. The bill

The secrets service holds every secret, so it needs its own access control, audit and backups. The application still needs a first credential
to prove who it is; platform identity, in the advanced track, removes it. Leases and rotation need applications that can reconnect with a new
login without a restart. A scanner finds only what it has patterns for, so it is one check, not a guarantee, and history must be scanned too.
The -with-openbao version runs the same leases against a real secrets server.

## The verdict

Keep secrets out of repositories and images, and scan both. Inject them at run time from files only the application can read, and never log
them. Prefer short-lived, per-application credentials from a secrets service, rotate with two valid versions during a changeover, and revoke
on any sign of a leak. Protect the secrets service as the most valuable system you run.

## How to recognise this in code you did not write

- `/run/secrets/...` paths and Kubernetes secret volumes.
- `vault read database/creds/...` or `bao lease renew` in scripts.
- Secret scanning alerts on a pull request.
- Key versions such as `v1` and `v2` in a provider's dashboard.

## Where you have already met this

- Docker and Kubernetes secrets mounted as files under /run/secrets.
- HashiCorp Vault and OpenBao, AWS Secrets Manager, Azure Key Vault and Google Secret Manager.
- gitleaks, trufflehog and GitHub secret scanning, which fail a build or block a push.
- Breach reports that begin with a key found in a public repository or an image.
