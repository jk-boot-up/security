# Backups and Recovery with RustFS, an S3-Compatible Object Store, Explained

## The idea in one sentence

Backups survive ransomware when old versions are kept, when locks stop anyone from deleting them before a date, and when the job that writes them can do nothing but add objects.

## The 5 acts

### 1. Versioning: an overwrite does not destroy the old data

The input: the bucket shop-orders with versioning on, and two writes to orders.csv. Version 1 is the shop's orders, 40 bytes; version 2 is ransomware's scrambled bytes, 25 bytes.
The result: 2 versions are kept. A normal read returns version 2, the scrambled one, but version 1, read by its id, is still the original. The restore copies version 1 back as version 3: a normal read is the original again, 3 versions are kept, and version 2 stays as evidence.
A plain delete adds 1 delete marker. A normal read now fails with 404 NoSuchKey, but version 1 is still there. Whoever may delete versions can still erase them all, which is what the next act stops.

### 2. Object Lock: nobody can delete a locked version

The input: the bucket shop-backups, created with object lock on, and two backups. backup-full.tar is locked in COMPLIANCE mode until now plus one day; backup-soft.tar in GOVERNANCE mode until the same time.
The result for the compliance version, tried by the root account: delete the version, refused with 403 AccessDenied; delete asking to bypass governance, refused; shorten the retention, refused. A plain delete is allowed, but only adds a marker, and the version still reads.
For the governance version: a plain delete of the version is refused, but with the bypass flag, which needs a special permission, it is allowed. Compliance cannot be lifted by anyone until the date, so choose it with care.

### 3. The backup job gets an identity that can only add objects

The input: the user backup-writer, with a policy of one statement. Effect Allow, Action s3:PutObject, Resource arn:aws:s3:::shop-backups/*: add objects to the backup bucket, and nothing else.
The result: the backup job's put of nightly.tar is allowed. Reading it back, listing its versions, deleting it, deleting a version, and writing into shop-orders are all refused with 403 AccessDenied.
So a thief who steals the backup job's key can add data, but cannot read the backups, change their history or destroy them. It can still add junk, so set a quota and alert on unusual uploads.

### 4. A checksum catches a damaged backup

The input: the SHA-256 of the backup bytes and of other bytes, in Base64, and two uploads. checked.tar sends the backup with its own checksum; damaged.tar sends the backup with the checksum of other bytes, as a transfer damaged on the way would.
The result: checked.tar is allowed, and damaged.tar is refused with 400 BadDigest. What is stored for checked.tar hashes to the same value, and damaged.tar left 0 versions behind.
The store checks the bytes it receives. Whether the backup is usable is a different question: a restore test is still needed.

### 5. Keeping history without keeping it for ever, and the cost

The input: one lifecycle rule on shop-orders, expire-old-versions, enabled: an old version is removed 30 days after it is replaced.
The cost, worked out: every version is stored in full. An orders.csv of 2 MB overwritten every hour keeps 24 x 30 = 720 old versions, and 720 x 2 MB = 1440 MB of storage for one 2 MB file. The rule keeps it bounded, not free.
The limits: a compliance date set wrong is paid for in storage, since nobody can lift it. The root account can change every policy, so keep it out of daily use. And a lock stops the API, not someone deleting the disks, so keep a copy somewhere else too.

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
