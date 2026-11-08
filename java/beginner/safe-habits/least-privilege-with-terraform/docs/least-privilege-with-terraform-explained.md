# Least Privilege with Terraform and LocalStack, Explained

## The idea in one sentence

Permissions written as code can be reviewed, counted and linted: a scoped policy keeps the blast radius small, an explicit deny and a boundary narrow it further, and a linter catches the shapes that grant more power than they seem to.

## The 5 acts

### 1. Policies written as code

The store's account offers 25 things to do, each an action on a resource: 8 in S3 (the file store), 10 in DynamoDB (the tables), 3 in IAM, 1 in STS, 1 in KMS and 2 in Lambda. One example is s3:GetObject on shop-reports/q3.csv: read one report file.
Before the fix, the nightly export wears the wildcard policy: one statement, Allow * on *. A stolen key reaches 25 of 25.
After the fix, it wears the exporter policy: three statements, list the reports bucket, read the files in it, and query the orders table. A stolen key reaches 3 of 25, which is exactly what the nightly export needs.

### 2. How a request is matched

The exporter's S3 part has two statements: Allow s3:ListBucket on shop-reports, and Allow s3:GetObject on shop-reports/*.
Reading shop-reports/q3.csv matches the second statement: ALLOW. Reading shop-secrets/keys.pem matches nothing: IMPLICIT_DENY. Listing shop-reports matches the first: ALLOW.
Listing with a file's name, shop-reports/q3.csv, is IMPLICIT_DENY, and reading the bucket shop-reports as if it were a file is IMPLICIT_DENY too, because /* means the files in the bucket, not the bucket itself. The action s3:getobject in small letters is still ALLOW: action names ignore case.
In a pattern, a star matches any letters, so s3:Get* matches s3:GetObject and not s3:PutObject. When no statement matches, the answer is denied by default.

### 3. A deny beats every allow

The admin_with_deny policy has two statements: Allow * on *, and Deny s3:* and kms:* on shop-secrets, shop-secrets/* and key/*.
Reading shop-secrets/keys.pem matches both statements, and the deny wins: EXPLICIT_DENY. Decrypting with key/shop is EXPLICIT_DENY too. Writing shop-reports/q3.csv matches only the allow: ALLOW.
With the two statements in the opposite order, all 25 answers are the same: order never matters. The count falls from 25 of 25 to 20 of 25. The deny is a safety net, and 20 things are still open.

### 4. A permissions boundary

The boundary allows s3:List*, s3:Get*, dynamodb:Scan, dynamodb:Query and dynamodb:GetItem on everything, and denies all of S3 on the secrets bucket.
The wildcard policy alone reaches 25 of 25. The same policy under the boundary reaches 8 of 25. The boundary alone, with no policy attached, reaches 0 of 25: a boundary grants nothing. Admin with a deny, under the boundary, is also 8 of 25.
The 8 things left are listing the reports bucket, reading the report file, and GetItem, Query and Scan on the orders and customers tables. A request is allowed only when a policy allows it AND the boundary allows it, and nothing denies it.

### 5. A linter, and the way to more power

The linter reads the text of each policy, not its effect. The wildcard and admin_with_deny policies each have ACTION_STAR and RESOURCE_STAR. The exporter has none. The boundary has RESOURCE_STAR. The deployer has RESOURCE_STAR and ESCALATION.
The deployer allows lambda:InvokeFunction and lambda:CreateFunction on function:checkout, and iam:PassRole on *. A stolen key reaches only 3 of 25, so it looks small.
But three steps lead to more power: iam:PassRole on * hands any role to a function, lambda:CreateFunction makes checkout with that role, and lambda:InvokeFunction runs code as that role. So read the policy as well as counting it.
This evaluator ignores resource policies and organisation rules, and it refuses a statement with a condition rather than guess.

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
