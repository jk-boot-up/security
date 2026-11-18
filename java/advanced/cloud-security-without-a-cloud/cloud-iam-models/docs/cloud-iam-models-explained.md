# Cloud IAM Models, Explained

## The idea in one sentence

A permission is the outcome of several documents that must agree, and the one that says no always wins.

## The 5 acts

### 1. Identity policies: default deny, allow, explicit deny

The input is ana's policy reports-read: Allow s3:GetObject and s3:ListBucket on bank-reports and bank-reports/*, and Deny s3:GetObject on bank-reports/secret/*. She makes six requests.
The result: reading a report is allowed (also as S3:GETOBJECT, because action names ignore case); reading the secret is an explicit deny; writing, reading another bucket, and reading Bank-Reports with capitals are implicit denies, because resource names keep their case.
How a request is decided: seven questions in a fixed order. For request 1 nothing denies, there is no organization policy, boundary, session policy or resource policy, the identity policy allows and the account is the same: ALLOW. For request 3 the first question already says yes, so the walk ends: DENY.
carol's policy is Allow with NotAction iam:*: deleting a bucket is allowed, creating a user is not.

### 2. Accounts: who has to agree

The input: ben, of the bank account 111111111111, asks s3:GetObject on shared-data/file.csv. His identity policy may allow it, the bucket policy may allow his account, and the bucket may sit in his account or in another.
The result, six cases: with the bucket in the same account, either policy alone allows (3 of 3); in another account only both together allow (1 of 3). Identity alone is denied because the bucket's account does not allow it; the bucket policy alone because ben's own account does not.
A bucket policy with Principal * lets someone with no account at all read. A stranger from another account still needs their own policy to allow it. An organization policy that only allows ec2:* denies s3:GetObject even to an administrator with Allow * on *.

### 3. Roles and the layers that narrow them

The input: the role Developer, whose trust policy allows the bank account to assume it only with aws:MultiFactorAuthPresent true, and whose policy allows s3:*, dynamodb:*, ec2:* and iam:PassRole. ben assumes it without and with a second factor.
The result: without it the trust policy does not allow it; with it ben gets a session. Eight actions are asked of the session: the role allows 6, a permission boundary of s3:* and dynamodb:* leaves 4, and a session policy of s3:GetObject and dynamodb:Query leaves 2. A session policy that allows * still cannot get iam:CreateUser.
A bucket policy that names the session itself allows s3:DeleteObject: the boundary and the session policy are skipped. Conditions on a tag and on the source address allow the payments team from the office network, and nobody else.

### 4. The confused deputy

The input: ReportCo, a reporting service in account 999999999999, reads each customer's files by assuming a role the customer made. The bank's role ReportCoAccess may read bank-reports/*, and its trust policy allows ReportCo's account.
Before the fix: the bank's own request through ReportCo is allowed, and so is Mallory's, who simply names the bank's role. ReportCo acts with its own rights for whoever asks: it is the confused deputy.
After the fix: ReportCo issued one external id per customer (the-bank ext-bank-7f3a, mallory-corp ext-mallory-91c2), and the trust policy asks for sts:ExternalId equal to ext-bank-7f3a. The bank's request sends that id: ALLOW. Mallory's request sends hers: DENY. She cannot choose the id, because ReportCo issues it.
Between services the same is done with aws:SourceArn: a topic that trusts the notifier publishes for any bucket, and with ArnLike arn:aws:s3:::bank-* only for the bank's buckets.

### 5. The bill

The input: five policies in the bank account. ana may read bank-reports/*; ben may pass roles and create and invoke functions; carol may assume DevRole; DevRole's DevPolicy allows s3:* and iam:CreatePolicyVersion; AdminRole has Allow * on *.
The result of the search: ana has no path. ben passes AdminRole to a function he creates and invokes, and the function runs as administrator. carol assumes DevRole, whose policy may write a new version of DevPolicy itself.
How carol's path is found, step by step: carol may assume DevRole (ALLOW); the session may create a new version of DevPolicy (ALLOW); version 2 says Allow * on *, and DevRole has it, so the session is an administrator (yes).
3 of 3 statements have a wildcard action or resource. This evaluator follows the documented rules for a handful of services; the real service has more, so a model finds the shape of a problem and the provider's policy simulator, or a test account, confirms it.

## The verdict

Start from deny, allow narrowly, use conditions for who and from where, put a boundary on roles that others create, require an external id from every third party that assumes a role, look for escalation paths as you would for bugs, and confirm the model against the real service.

## How to recognise this in code you did not write

- `"Effect": "Allow"` with `"Action": "*"`.
- A trust policy that names an account root.
- `sts:AssumeRole` in a vendor's setup guide.
- An external id field in an integration form.

## Where you have already met this

- A JSON policy with `"Action": "*"` that was "temporary".
- An error that says "is not authorized to perform".
- A role that a vendor asked you to create so that they can "connect".
- A bucket that turned out to be public.
