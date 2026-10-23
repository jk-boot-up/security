# Ransomware Resilience, Explained

## The idea in one sentence

A backup the attacker can delete or poison is not a backup; lock it, separate it, copy it elsewhere, check what goes in, and practise the restore.

## The 6 acts

### 1. A plain bucket

The bank keeps 24 hourly ledger snapshots in a bucket with no versioning and no lock, written by a service account that can do everything. The attacker uses that account to overwrite every backup with random bytes: 24 of 24 are accepted, 0 of 24 restore with a matching SHA-256, and then the deletes are accepted 24 of 24 times and no object is left.

### 2. Versioning only

With versioning every overwrite adds a version: 48 versions, 24 of 24 backups restore when the restore looks through the versions and 0 of 24 when it takes the latest. A delete of a key only adds a delete marker (24 markers, 48 versions still there). A permanent delete by version id is accepted 48 of 48 times and leaves 0 versions and 0 of 24 restorable backups.

### 3. Object lock in COMPLIANCE mode

The bucket has object lock with a default retention of 1 day in COMPLIANCE mode (30 days or more in production). The backup credentials, the governance-bypass header and the root administrator are all refused with AccessDenied on 24 of 24 versions. An overwrite by key is accepted as a new version and the new garbage versions are locked too (48 of 48 refused), so 24 of 24 backups restore by version. Shortening the retention and changing COMPLIANCE to GOVERNANCE are AccessDenied, suspending versioning is InvalidBucketState and deleting the bucket is BucketNotEmpty. Root can extend a lock and can remove the default rule, which leaves the 24 stored versions locked but not new backups, unless the writer sets the retention on each object.

### 4. Isolation and an air-gapped copy

The backup writer may add objects and list; the recovery account may read every version; nobody else is needed. The writer is refused a version delete, a key delete, a read, a retention change, a legal hold, a bucket delete and another bucket (AccessDenied each); the recovery account is refused a write and a delete. A policy cannot stop the writer from overwriting a key it wrote, which is why the lock matters. The 24 checked backups are copied to a second store whose credentials the first store does not know: the first store's root and the writer get InvalidAccessKeyId there, and a disconnected store answers nothing at all.

### 5. Ransomware and the poisoned backup

A simulation encrypts the 9 files of the data directory with AES-256-GCM at 09:20 and deletes the originals. The unguarded job backs up 11 objects at 10:00; they match their own manifest 10 of 10 and look like a ledger 1 of 10 (the ransom note), and they cannot be deleted for the retention. The guard (canary file, file entropy, 4 KiB block entropy) refuses the run and writes 0 objects. Ransomware that skips the canary is caught by entropy, half-encrypted files only by the block test, and a compressed export is a false alarm. Restore picks the newest run that passes both checks: 09:00, identical for 9 of 9 files, with 3000 transactions lost at 1800 an hour.

### 6. The restore exercise

The exercise restores the 24 backups into a fresh directory, checks every SHA-256 against the manifest and the ledger total, and counts 120 steps. The recovery time is computed, not timed: 15 minutes to declare, 45 to build a clean environment, 120 steps of 3 seconds and 24 backups of 25 GiB at 400 MiB/s, which is 91.6 minutes; after the first store is destroyed the copy at 100 MiB/s gives 168.4 minutes. Both meet the 240 minute target. The recovery point depends on the schedule: 19200, 4800, 3000 and 2550 transactions lost for backups every 1440, 240, 60 and 15 minutes.

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
