# Cloud IAM Models, Explained

## The idea in one sentence

A permission is the outcome of several documents that must agree, and the one that says no always wins.

## The 5 acts

### 1. Identity policies: default deny, allow, explicit deny

ana's policy allows reading bank-reports and denies reading bank-reports/secret/*. Reading a report is allowed (also with S3:GETOBJECT, because action names ignore case); reading the secret is an explicit deny; writing, reading another bucket, and reading Bank-Reports with a capital are implicit denies (resource names do not ignore case). carol's policy is Allow with NotAction iam:*: deleting a bucket is allowed, creating a user is not.

### 2. Accounts: who has to agree

Six cases of a caller in the bank account. A bucket in the same account: an identity policy alone, a bucket policy alone, or both, allow. A bucket in another account: both allow; an identity policy alone is denied because the resource's account does not allow it; a bucket policy alone is denied because the principal's own account does not. A bucket policy with Principal * lets someone with no account at all read; a stranger from another account whose own policies allow nothing cannot, and with a policy of their own that allows it, can. An organization policy that does not allow s3:GetObject stops even an administrator.

### 3. Roles and the layers that narrow them

ben assumes Developer, whose trust policy asks for a second factor: without it the trust policy does not allow it; with it the trust policy and ben's own policies allow it. Of eight actions the role's own policy allows 6. With a permission boundary of s3:* and dynamodb:* 4 are left; with a session policy passed at assumption, 2; a session policy can only narrow, so asking for everything changes nothing. A bucket policy that names the session itself allows s3:DeleteObject, which neither the boundary nor the session policy would. Conditions on a tag and on the source address allow the payments team from the office network and nobody else.

### 4. The confused deputy

ReportCo reads its customers' buckets by assuming a role each customer made, whose trust policy names ReportCo's account. The bank's own request through ReportCo is allowed. Mallory, another customer, types the bank's role into ReportCo's form: allowed, the service is the deputy and it has been fooled. The trust policy now also asks for the external id that ReportCo issued to the bank: the bank's request is allowed, Mallory's, which carries the id issued to Mallory, is denied. An id that Mallory chooses is no use: it must be issued by the service, one for each customer, and not guessable. Between services the same is done with aws:SourceArn in the condition: a topic that trusts a service publishes for any bucket that asks, and only for the bank's buckets with the condition.

### 5. The bill

Who can make themselves an administrator, asking the evaluator at every step: ana, no path found. ben passes the admin role to a function he creates and runs (iam:PassRole, lambda:CreateFunction, lambda:InvokeFunction). carol assumes a developer role that may rewrite its own policy (iam:CreatePolicyVersion on DevPolicy). Three of three policies have a wildcard action or resource. This evaluator follows the documented rules for a handful of services; the real service has more, so a model like this finds the shape of a problem and the real service's policy simulator, or a real test account, confirms it.

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
