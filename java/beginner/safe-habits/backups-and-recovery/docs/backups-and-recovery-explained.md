# Backups and Recovery, Explained

## The idea in one sentence

A backup is complete when its manifest says so, trustworthy only after a restore test, safe from ransomware only if one copy cannot be changed by the shop's own account, and a secret itself, so it is sealed and its key is kept apart.

## The 5 acts

### 1. Full and incremental backups, with a manifest

Day one is a full backup: three files stored. On day two the orders file changed and a refunds file appeared, so the incremental backup stores just those two. On day three only the customers file changed: one file stored.
The manifest of day three lists all four files, each with its SHA-256. Restoring day three takes the newest copy of each file from the chain, and comparing with the manifest finds no problems.
Incremental backups are small and quick, and a restore needs the whole chain.

### 2. Can the backup be trusted? Only a restore test says

One byte of a stored file is changed: the check finds one changed file, and the manifest names it. A stored file is deleted: one missing file is found. But a backup job that skipped the orders file because it was in use
produces a manifest that checks out, since the manifest only describes what was copied. The restore test asks the application's own question, is there a usable orders file, and the answer is no. A backup nobody restored is a hope.
A good backup passes the restore test, with three orders.

### 3. The 3-2-1 rule, and how long to keep backups

The 3-2-1 rule asks for three copies, on two kinds of media, one of them off site, and today also one that cannot be changed. A copy in another folder of the same server scores zero of four.
A server, an office NAS and an off-site locked bucket score four of four. Retention decides how long to keep: out of 400 daily snapshots, keeping seven daily, four weekly and twelve monthly leaves 21 and prunes 379.
The oldest kept is the first of March the year before: a year of history in 21 snapshots. Keeping less than the law or the business needs is a risk, and so is keeping personal data for ever.

### 4. Ransomware, and the copy it cannot change

Ransomware runs on the shop server and encrypts three files in place. The backup folder was mounted and writable, so it is encrypted too: the backup went with the data. A store that only ever adds new versions took the attacker's writes as new versions:
two versions of the orders file, and restoring the version from before the attack gives every original back. Files marked read-only stop the attack: zero encrypted, three blocked. But the same user can make them writable again, and then three are encrypted.
Real protection comes from a copy the shop's own account cannot change.

### 5. How much can be lost, how long it takes, and backups as secrets

With a nightly backup at two in the morning, a failure at half past three, noticed at ten to four, and a restore of 95 minutes, the recovery point is 13:30 of work lost, about 162 orders at 12 an hour, and the recovery time is 01:55 until the shop is back.
With an hourly backup the recovery point is half an hour and about six orders; the recovery time is the same. And since a backup holds every customer's data, it is sealed with AES-256-GCM: the archive shows no customer name, the right key opens it, another key is refused,
and the key file is readable by its owner only and lives apart from the backups. A lost key is a lost backup.

## The verdict

Back up on a schedule that matches what the business can lose, follow 3-2-1 with an unchangeable copy, restore regularly into a scratch environment, prune by a written policy, and encrypt the archive with a key that lives somewhere else.

## How to recognise this in code you did not write

- `restic`, `borg`, `rsync` and database dump jobs in cron.
- Object lock, versioning and write-once settings in object stores.
- RPO and RTO in disaster recovery plans.
- A "restore drill" in a runbook.

## Where you have already met this

- `rsync`, `restic`, `borg` and cloud snapshot features.
- Object lock and write-once storage in cloud object stores.
- Disaster recovery plans and "RPO" and "RTO" in service agreements.
- News stories about ransomware that also took the backups.
