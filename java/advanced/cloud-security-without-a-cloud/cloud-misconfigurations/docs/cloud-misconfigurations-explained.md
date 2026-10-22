# Cloud Misconfigurations, Explained

## The idea in one sentence

Most cloud breaches are settings, so look for the settings in the code before deploy and in the account afterwards.

## The 5 acts

### 1. The first version

Three buckets: bank-statements with a public-read ACL, bank-exports with a policy for everyone, and bank-audit, which is fine. A policy that allows every action on every resource, a web group with 22 and 443 open to the world, a database group with 5432 open, a server with the metadata service answering without a token and an unencrypted disk. The scanner reports 13 findings: 3 critical, 5 high, 3 medium... of nine kinds of rule.

### 2. The fixes

The buckets get a public access block, encryption and versioning; the policies are replaced; the ingress rules are revoked; the server and the disks are deleted and created again, because the metadata options and the encryption of a disk cannot be changed in place. The scanner reports nothing.

### 3. Before deploy: Checkov and Trivy

The setup written as main.tf in two versions. Both scanners fail the first. Of our nine rules, 7 are found by both; Checkov alone finds the wildcard policy and the public bucket policy, and Trivy has no check for either. On the fixed files none of the nine problems is left except encryption: both tools ask for a customer-managed key where the rule accepts AES256.

### 4. The metadata endpoint

A server on a loopback port answers like the metadata service in both versions. A web feature that fetches a link on the visitor's behalf gets the credentials from version 1 and a 401 from version 2. A feature that lets the visitor choose the method and a header does the PUT for a token, then the GET: version 2 hands over the credentials too. A request that came through a proxy is refused with 403 in both.

### 5. The bill

Two of the nine fixes replace a resource. A scanner sees the setting, not the intent: a public bucket for a website needs a written exception with an owner and a date. Moto stores configuration but does not enforce it the way AWS does, so the emulator tests the code, not the cloud. Scan the files before deploy and the account on a schedule, because code and the cloud drift apart.

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
