# Backups and Recovery with RustFS, an S3-Compatible Object Store, Explained

## The idea in one sentence

Backups survive ransomware when old versions are kept, when locks stop anyone from deleting them before a date, and when the job that writes them can do nothing but add objects.

## The 5 acts

### 1. Versioning: an overwrite does not destroy the old data

The shop stores orders.csv and ransomware overwrites it with scrambled bytes. Two versions are kept. A normal read returns the scrambled file, but the original is still there as an older version. Restoring is writing the older version back as the newest.
The scrambled version stays in the history as evidence. A plain delete only adds a delete marker: reading the key fails, the data is not gone. But an attacker with the right to delete versions could still remove every version, which is what the next act stops.

### 2. Object Lock: nobody can delete a locked version

A backup is stored with COMPLIANCE retention for a day. Deleting that version is refused. Deleting it while asking to bypass governance is refused. Shortening the retention is refused. A plain delete is allowed but only adds a marker, and the locked version is still readable.
GOVERNANCE is softer: deleting is refused, but with the special bypass permission it can be lifted. COMPLIANCE cannot be lifted by anyone until the date, so choose it with care.

### 3. The backup job gets an identity that can only add objects

The backup job gets its own identity with a policy that has one action, adding objects to one bucket. It can write a backup. It cannot read one back, list versions, delete, delete a version, or write to the orders bucket.
So a thief who steals the backup job's key can add data but cannot read it, change history or destroy backups. It can still add junk, so quotas and alerts on unusual uploads matter.

### 4. A checksum catches a damaged backup

Uploading with the SHA-256 of the data is allowed. Uploading bytes whose SHA-256 does not match the one sent is refused, and leaves nothing behind. What was stored for the good upload hashes to the same value.
The store checks bytes in transit and at upload; a restore test is still needed to know that a backup is usable.

### 5. Keeping history without keeping it for ever, and the cost

A lifecycle rule on the orders bucket removes old versions 30 days after they are replaced. Every version is stored in full, so storage grows with every overwrite until the rule removes them. A COMPLIANCE lock cannot be undone. The root account is the real key, so keep it out of daily use.
Object lock stops the API from deleting backups; it does not stop someone from deleting the disks, so keep a copy somewhere else too.

## The verdict

Use a versioned bucket, a write-only identity for the backup job, a lifecycle rule and, for the backups that matter most, Object Lock with a retention you can afford. Test restores regularly, protect the root account, and keep a copy outside the account.

## How to recognise this in code you did not write

- S3 Versioning, Object Lock and bucket policies in cloud consoles.
- `mc admin policy` and `mc retention` in guides for S3-compatible stores.
- Backup software settings called "immutable backups" or "WORM".

## Where you have already met this

- Amazon S3 Versioning and Object Lock, Azure immutable blob storage, Google Cloud bucket lock.
- Backup tools such as restic and Veeam, which can write to S3 buckets with locks.
- The advice "keep an immutable copy" in every ransomware guide.
- RustFS, Ceph and other S3-compatible stores in on-premises and home-lab backups.
