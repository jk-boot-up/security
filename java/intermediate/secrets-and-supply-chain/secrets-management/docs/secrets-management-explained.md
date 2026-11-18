# Secrets Management, Explained

## The idea in one sentence

Secrets should never live in code, images or environment variables; they should be injected at run time, never logged, and ideally leased per
application for a short time from a secrets service that can rotate and revoke them, which then becomes the thing to protect.

## The 5 acts

### 1. Where secrets leak

The input is the three files the team committed: config/application.properties, config/production.properties and scripts/refund.sh.
The scanner finds three secrets: db.password and payment.api-key, by the name of the setting, and a random-looking token in the script.
It skips ${DB_PASSWORD} and file:/run/secrets/payment-key, because a placeholder only says where a secret comes from.
How "random-looking" is worked out: entropy is the sum, over each character, of p x log2(1/p), where p is the character's share.
The 32-character token has 21 characters once, 4 twice and 1 three times: 3.28 + 1.00 + 0.32 = 4.60 bits, at least 4.0, so it is flagged.
The class name ProductCatalogueController gives 0.90 + 1.14 + 1.08 + 0.42 = 3.54, under 4.0, so it is not.
A key in the environment variable SHOP_PAYMENT_KEY is read by printenv, a program the store starts, and by any crash report that prints the environment.
An image is built in three layers: the jar, the key, and a whiteout that hides the key. The running container sees only app/shop.jar, but layer 2 still holds the key.

### 2. Secrets injected at run time

Before the fix, the mounted file run/secrets/db-password has permissions rw-r--r--: owner rw-, group r--, others r--. Group and others can read it, so the store refuses it at startup.
After the fix, the same file has rw-------: owner rw-, group ---, others ---. It is read once at startup: 24 characters, not in the code or the image.
The code writes "connecting with password " + password into a log line; the log shows connecting with password [redacted], because the Secret type never prints its value.
Once the connection pool has its copy, the startup copy is wiped, and using it fails.

### 3. A secrets service that hands out leased logins

The requests: at t=0, shop-api with its credential gets v-shop-api-1 until t=900, reports gets v-reports-2 until t=900, and shop-api with a wrong credential is refused.
At t=600 v-shop-api-1 works. A renewal's new end is min(now + time to live, issued + maximum): at t=600, min(600 + 900, 0 + 3600) = 1500.
At t=900 the reports lease ends, never renewed: its login is removed, and v-reports-2 no longer works.
The shop renews at t=1400, 2100, 2800 and 3500: 2300, 3000, then min(3700, 3600) = 3600 twice, because a lease never runs past its maximum.
At t=3600 v-shop-api-1 no longer works, so the shop asks for a new lease. The audit trail records all 10 requests.

### 4. Rotation without downtime, and the blast radius of a leak

The rotation in three steps: with v1 in use the provider accepts [v1]; it issues v2 and accepts [v1, v2], so both work during the changeover; once every server is on v2, v1 is revoked and only [v2] is accepted.
The blast radius of a leak: a thief copies a leased login at t=600. It ends at t=900, so it works 900 - 600 = 300 s more; if the thief can renew it, at most until t=3600, 3600 - 600 = 3000 s more.
Once the leak is noticed, revoking the lease ends it at once: the login works right after: false.
A static database password copied at the same time still works three years later, until someone changes it.

### 5. The bill

The secrets service holds every secret, so it must be guarded, audited and backed up. The application still needs a first credential to prove who it is; platform identity, in the advanced track, removes it.
Leases and rotation need applications that can take a new login without a restart. A scanner finds only what it has patterns for, so the history of a repository and every image must be scanned too.
The -with-openbao version runs the same leases on a real secrets server.

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
