# Cloud Misconfigurations, Explained

## The idea in one sentence

Most cloud breaches are settings, so look for the settings in the code before deploy and in the account afterwards.

## The 5 acts

### 1. The first version, deployed to the emulator

The input is what the scanner read back from Moto through the SDK: bank-statements readable by everyone through its ACL, bank-exports through its bucket policy, both with no public access block, no encryption and no versions; bank-audit blocked, AES256 and versioned. The policy developer-everything allows * on *; the web group opens 443 and 22 to 0.0.0.0/0 and the database group 5432; the web server's metadata tokens are optional with a hop limit of 2; 2 disks, 0 encrypted.
The result is 13 findings, worst first. The sum: 3 CRITICAL + 5 HIGH + 3 MEDIUM + 2 LOW = 13. Nine rules fired; four of them fired twice (the three bucket protections on both open buckets, and an admin port on both groups), so 9 + 4 = 13.

### 2. The fixes, made with the same SDK

The input is the 13 findings. The fixes are SDK calls, each logged as it is made: the ACL set to private, the public bucket policy deleted, a full public access block, AES256 and versions on both buckets, a new policy version that allows only s3:GetObject on bank-statements, ports 22 and 5432 closed, and the metadata options switched to tokens required with a hop limit of 1, in place.
A disk cannot be encrypted after it was made, so the data disk is replaced by an encrypted one and the server by a new one with an encrypted root disk: 9 changes = 7 edits + 2 replacements. The scanner reads the account again: every bucket blocked, AES256 and versioned, no world-open admin port, tokens required, 2 disks and 2 encrypted. 0 findings.

### 3. Before deploy: the same setup as files, read by two scanners

The input is main.tf in two versions: the first with 9 resources (public buckets, Action *, 0.0.0.0/0), the fixed one with 13 (access blocks, encryption and versions added). Checkov runs from a pinned pip install and Trivy from a pinned image with its built-in checks, so the counts do not change with a download: Checkov fails 37 checks on the first files and 16 on the fixed ones; Trivy 26 and 5.
For each of our nine rules, the table shows the first check each tool fired, or - when it has none. Found by both: 7 of 9. Checkov only: s3-public-policy and iam-wildcard-admin. Port 22 on web is caught by both, but port 5432 on database by neither: their rules list SSH and RDP ports only. In the fixed files, both still fire for s3-no-encryption, because CKV_AWS_145 and AWS-0132 want a customer-managed key, while our rule accepts AES256; the other advice (14 and 3) is logging, monitoring and the like.

### 4. The metadata endpoint, reached through the web server

The metadata service runs on a loopback port in both versions. The input of the first request is a plain GET of /latest/meta-data/iam/security-credentials/web-role, made by a feature that fetches a link for the visitor; version 1 answers 200 with the credentials, version 2 answers 401 because there is no token.
A webhook tester lets the visitor choose the method and one header: a PUT to /latest/api/token with a TTL header gets a token of 36 characters, and a GET with X-aws-ec2-metadata-token gets the credentials from version 2 too. Behind a proxy that adds X-Forwarded-For, version 2 refuses the token PUT with 403 and the GET without a token with 401; version 1 still answers 200. A hop limit of 1 also stops a token from reaching a container one network hop further.

### 5. What the scanners and the emulator cannot tell

The input is two calls on bank-audit, which has a full public access block: PutBucketPolicy with Principal * for s3:GetObject, and PutBucketAcl public-read. Moto answers 200 to both; AWS would answer 403 AccessDenied, because it enforces the block. So the emulator tests the code, not the cloud: the scan of the account afterwards finds s3-public-acl and s3-public-policy on bank-audit, 2 CRITICAL.
What is left for people: a disk is encrypted only when it is made, so plan replacements; a public website bucket looks like a leak to every scanner and needs a written exception with an owner and a date; and code and the account drift apart, so scan the files before deploy and the account after.

## The verdict

Block public access by default, give every identity only what it needs, close administrative ports, require IMDSv2 and encrypt disks, scan the files in the build and the account on a schedule, and write down every exception with an owner and a date.

## How to recognise this in code you did not write

- `Principal: *` in a bucket policy.
- `Action: *` with `Resource: *`.
- `cidr_blocks = ["0.0.0.0/0"]` on port 22.
- `http_tokens = "optional"`.

## Where you have already met this

- A bucket opened for a quick demo and never closed.
- A policy with Action star.
- A security group with 0.0.0.0/0 on SSH.
- A scanner report of hundreds of findings that nobody reads.
