# Ransomware Resilience, Explained

## The idea in one sentence

A backup the attacker can delete or poison is not a backup; lock it, separate it, copy it elsewhere, check what goes in, and practise the restore.

## The 7 acts

### 1. A plain bucket

The input: 24 hourly ledger snapshots of 400 rows each, a manifest of one SHA-256 per backup kept outside the store, and a bucket with no versioning and no lock, written by a service account allowed s3:* on every bucket.
Before the attack 24 of 24 backups restore with a matching SHA-256.
The attack: with the same credentials every key is overwritten with random bytes (accepted 24 of 24). One version is kept per backup, so 0 of 24 restore. Then every key is deleted (accepted 24 of 24) and 0 objects are left.

### 2. Versioning only

The input: the same 24 backups and the same all-powerful account, in a bucket with versioning on.
The overwrite is accepted 24 of 24 and adds a version: 48 in the bucket, 24 good and 24 garbage. A restore that takes the latest version gets 0 of 24; one that looks through the versions gets 24 of 24.
A delete by key only adds 24 delete markers; the 48 versions are still there. But a delete by version id is permanent: accepted 48 of 48, 0 versions left, 0 of 24 backups restore.

### 3. Object lock in COMPLIANCE mode

The input: a bucket with object lock and a default retention of 1 day in COMPLIANCE mode (30 days or more in production); each stored backup reports the mode COMPLIANCE.
Deleting each of the 24 versions by id is refused with AccessDenied, 24 of 24, for the backup credentials, with the governance-bypass header, and for the root administrator. An overwrite by key is accepted as a new version, and the garbage is locked too: deleting all 48 versions is refused 48 of 48. Looking through the versions, 24 of 24 restore.
Root cannot loosen the lock: shortening the retention and changing COMPLIANCE to GOVERNANCE are AccessDenied, suspending versioning is InvalidBucketState and deleting the bucket is BucketNotEmpty; extending it is ok. The gap: root can remove the default rule. The 24 stored versions stay locked, but a backup written afterwards can be deleted, unless the writer sets the retention on each object.

### 4. Isolation and an air-gapped copy

The input: two policies on the locked vault bucket. backup-writer may s3:PutObject and s3:ListBucket; recovery may s3:GetObject, s3:GetObjectVersion, s3:ListBucket and s3:ListBucketVersions.
The writer stores the 24 backups, and is refused 7 of 7 other actions with AccessDenied: delete a version, delete a key, read a backup, change a retention, place a legal hold, delete the bucket and touch another bucket. The recovery account may read and list, and is refused a write and a delete. A policy cannot stop the writer from overwriting a key it wrote (ok, then ok); the lock keeps both versions, and 24 of 24 vault backups restore by version.
The second store has its own credentials. 24 of 24 backups are checked against the manifest and copied, and 24 of 24 restore from it. The first store's root and the writer's keys get InvalidAccessKeyId there, and while it is disconnected even its own root gets no answer.

### 5. The guard: a canary file and entropy

How entropy is worked out on one block of 4096 bytes: H is the sum, over the byte values that occur, of -p x log2(p), where p = count / 4096. The ledger text uses 18 byte values; the largest terms are '1' (633 times, p 0.1545, term 0.4163), ',' and '0', and H = 3.93 bits per byte. The same block encrypted with AES uses all 256 byte values, each about 28 times (p 0.0068, term 0.0492), and H = 7.95.
The guard has three detectors, and each one alone stops the backup: the canary file's SHA-256 changed, the entropy of a whole file is above 7.5, or the entropy of any 4 KiB block is above 7.5.
On five directories of 9 files: a healthy one is allowed; AES on every file trips all three; with the canary skipped by name, the entropy still catches it; AES on the first half of each file hides from the whole-file test but not from the block test; and a healthy directory with a compressed export is a false alarm, because compressed data looks like ciphertext.

### 6. Ransomware and the poisoned backup

Before the infection: hourly runs from 06:00 to 09:00 into two locked buckets, 40 objects in the unguarded one (4 runs x (9 files + 1 MANIFEST)).
At 09:20 a simulation encrypts the 9 files with AES-256-GCM, deletes the originals and leaves a ransom note: 10 files. The 10:00 backup without the guard writes 11 objects; with the guard it is refused and writes 0, because all three detectors fire.
The poisoned backup matches its own manifest 10 of 10, because the manifest was made from the encrypted files, but only 1 of 10 files looks like a ledger (the short ransom note). It cannot be deleted to save space: refused 11 of 11.
The restore takes the newest run that passes both checks: in the unguarded bucket it sees 2 runs, one fails the content test, and it chooses run-0900, identical to the directory at 09:00 for 9 of 9 files.
How the data lost is worked out: from the last good backup at 09:00 to the cut-off at 10:40 is 100 minutes; 100 x 1800 / 60 = 3000 transactions lost. Backing up every 1440, 240, 60 or 15 minutes loses 19200, 4800, 3000 or 2550.

### 7. The restore exercise

Restore 1 reads the vault bucket of the first store into a fresh directory: 24 of 24 backups good, 233558 bytes, 9600 rows, the ledger total matches, and 120 steps (24 backups x 5: list, get, write, hash, compare).
How the recovery time is worked out, from what the restore really did and a fixed model of production size and speed: 15 minutes to declare the incident, 45 to build a clean environment, 120 steps x 3 s = 360 s (6.0 minutes), and 24 x 25 GiB = 614400 MiB at 400 MiB/s = 1536 s (25.6 minutes). Total 91.6 minutes; the 240 minute target is met.
Then the first store is destroyed and answers unreachable. Restore 2 from the air-gapped copy is just as good, but over a 100 MiB/s link the transfer is 6144 s (102.4 minutes), so the total is 168.4 minutes, still within the target.
Not covered by the lock: root on the host of a store, a retention shorter than the time an attacker stays unseen, and data already encrypted when the oldest backup was made.

## The verdict

Use object lock in COMPLIANCE mode for data that cannot be recreated, give the backup job only the right to add objects, keep a recovery account that can only read, copy to a store with other credentials, check the data before it is backed up and after it is restored, and practise a timed restore with a computed recovery time and point.

## How to recognise this in code you did not write

- A backup account with full access.
- Versioning mistaken for immutability.
- A nightly job that backs up whatever is there.
- A restore plan nobody has run.

## Where you have already met this

- Backups on a share that the same admin account can delete.
- A backup job that copies encrypted files every night.
- A restore that has never been tried.
- A retention period nobody checked against the time an attacker stays unnoticed.
