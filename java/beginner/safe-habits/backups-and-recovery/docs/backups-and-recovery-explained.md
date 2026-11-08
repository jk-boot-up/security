# Backups and Recovery, Explained

## The idea in one sentence

A backup is complete when its manifest says so, trustworthy only after a restore test, safe from ransomware only if one copy cannot be changed by the shop's own account, and a secret itself, so it is sealed and its key is kept apart.

## The 5 acts

### 1. Full and incremental backups, with a manifest

The input: the shop's three files on day 1, and the changes on the next two days. On day 2 the orders file gets order 1003 and a refunds file appears; on day 3 the customers file gets a new customer.
The result: day 1 is a full backup and stores all 3 files. Day 2 is incremental and stores only the 2 that changed or appeared. Day 3 stores just 1. The manifest of day 3 lists all 4 files with the SHA-256 of each, shown here as the first 16 hex digits.
Restoring day 3 takes each file from the newest backup that stored it: customers from day 3, orders and refunds from day 2, inventory from day 1. Compared with the manifest, there are 0 problems. Incremental backups are small and quick, and a restore needs the whole chain.

### 2. Can the backup be trusted? Only a restore test says

The input: two kinds of damage to a stored backup, one byte of orders.csv in day 2 changed and inventory.csv in day 1 deleted. And two backups to test: a partial one, whose job skipped orders.csv because it was in use, and a good one.
The result: the manifest check names both, orders.csv changed and inventory.csv missing. But the partial backup's manifest checks out, because a manifest only describes what was copied. The restore test restores into a scratch folder and asks the shop's own question: is there a usable orders file? For the partial backup the answer is missing, 0 orders, failed. The good backup is usable, with 3 orders, and passes.
A backup nobody restored is a hope.

### 3. The 3-2-1 rule, and how long to keep backups

The 3-2-1 rule asks for three copies, on two kinds of media, one of them off site, and today also one that cannot be changed: four checks.
Before the fix: the shop server and another folder on the same server, both on the same disk. Two copies, one medium, nothing off site, nothing locked: 0 of 4. After the fix: the server, an office NAS on a disk, and a locked bucket in another region: 4 of 4.
Retention decides how long to keep. The input: 400 daily snapshots from 2025-01-01 to 2026-02-04, and the policy keep 7 daily, 4 weekly (the Sundays) and 12 monthly (the 1st of each month). The result: 7 daily, 4 weekly and 12 monthly, minus 2 that are in two lists (2026-02-01 is a Sunday and the 1st), so 21 are kept and 379 pruned. The oldest kept is 2025-03-01: a year of history in 21 snapshots. Keeping too little is a risk, and so is keeping personal data for ever.

### 4. Ransomware, and the copy it cannot change

The input: four copies of the shop's three files. The shop server and a mounted backup folder, both writable. A versioned store that can only add versions, holding the files at time 100. And a read-only folder, with permissions r--------. At time 200 the ransomware encrypts every file it can write.
The result: the shop server's 3 files are encrypted, and the orders file no longer reads as before. The mounted backup is encrypted too: the backup went with the data. The versioned store took the attacker's writes as a new version, so it has 2 versions of orders.csv, at 100 and 200. The read-only folder: 0 encrypted, 3 blocked.
The recovery: the versioned store's version from before 200 gives every original file back. But the same user can run chmod u+w on the read-only folder, and then all 3 are encrypted. Real protection comes from a copy the shop's own account cannot change.

### 5. Recovery point, recovery time, and a sealed backup

The input: the last nightly backup at 02:00, the order database fails at 15:30, someone notices at 15:50, the restore takes 95 minutes, and the shop takes 12 orders an hour.
The recovery point objective, RPO, is the work that can be lost: 15:30 - 02:00 = 13:30, that is 13.5 hours x 12 = 162 orders. The recovery time objective, RTO, is how long until the shop is back: 15:50 + 95 minutes = 17:25, and 17:25 - 15:30 = 01:55. With an hourly backup, the last at 15:00, the RPO is 00:30, 0.5 x 12 = 6 orders, and the RTO is still 01:55: more backups do not make a restore faster.
And a backup holds every customer's data, so it is sealed. The input: 3 files with names such as Ana, the archive name nightly-day-30 and a random 256-bit key; AES-256-GCM with the name bound in as associated data. The result: the archive does not show the name Ana; the right key opens it, 3 files; another key is refused; the same archive offered as nightly-day-31 is refused, because its name is sealed in; and the key file is rw------- and kept apart. A lost key is a lost backup, so the key needs its own copy.

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
