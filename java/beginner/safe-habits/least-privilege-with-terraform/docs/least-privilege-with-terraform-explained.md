# Least Privilege with Terraform and LocalStack, Explained

## The idea in one sentence

Permissions written as code can be reviewed, counted and linted: a scoped policy keeps the blast radius small, an explicit deny and a boundary narrow it further, and a linter catches the shapes that grant more power than they seem to.

## The 5 acts

### 1. Policies written as code

The store's account offers 25 things to do: an action on a resource, such as reading one report or querying one table. The wildcard policy, with one statement, lets an identity do all 25. The exporter policy, with three statements, lets it do 3, which is all the nightly export needs.
The deployer policy allows 3 too. The number of things an identity can do if its key is stolen is its blast radius.

### 2. How a request is matched

The exporter reads a report file: allowed. It reads a secret file: denied by default. It lists the reports bucket: allowed. Listing with a file's name instead of the bucket's is denied, and reading the bucket itself as if it were a file is denied too, because the slash-star
in the policy means the contents and not the bucket. Action names ignore case. The pattern s3:Get* matches s3:GetObject and not s3:PutObject. Anything not allowed is denied by default.

### 3. A deny beats every allow

An admin policy with an explicit deny on the secrets can do 20 of 25 things, against 25 without the deny. Reading the secret file is an explicit deny, while writing a report is allowed. With the statements in the opposite order the answer is the same.
The deny is a safety net, and it still leaves 20 things open.

### 4. A permissions boundary

The wildcard policy alone allows 25 of 25. The same policy under the boundary allows 8 of 25. The boundary alone, with nothing attached to the identity, allows 0: a boundary grants nothing. Admin with a deny, under the boundary, is also 8 of 25.
An identity gets only what both its policies and its boundary allow.

### 5. A linter, and the way to more power

A linter reads the text of each policy. It finds the star action and star resource in the wildcard and admin policies, nothing in the exporter, and in the deployer policy iam:PassRole on every resource. The deployer can do only 3 of 25 things and is still dangerous:
passing any role to a new function runs code as any role in the account. The count hides escalation, so read the policy as well as counting it. This evaluator ignores conditions, resource policies and organisation rules, and refuses a statement with a condition.

## The verdict

Write permissions as code, give each job a scoped policy, put a boundary on identities that people can create, count the blast radius, and lint for wildcards and for ways to grow one's own power. Test enforcement against a real account before relying on it.

## How to recognise this in code you did not write

- `data "aws_iam_policy_document"` blocks in Terraform repositories.
- Policy validation and unused-access findings in cloud consoles.
- `iam:PassRole` with `Resource: *` in security reviews.

## Where you have already met this

- `aws_iam_policy_document` and `aws_iam_policy` in Terraform repositories.
- IAM Access Analyzer and "policy validation" findings in cloud consoles.
- `"Action": "*"` and `"Resource": "*"` in blog posts, and in breach reports.
- Terraform plans reviewed in pull requests.
