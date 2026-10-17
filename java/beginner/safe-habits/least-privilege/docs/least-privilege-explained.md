# The Principle of Least Privilege, Explained

## The idea in one sentence

Give each person and program only the permissions its work needs, deny everything else by default, require two people for risky actions, let temporary access expire, and remove permissions nobody uses, so that any one stolen account can reach as little as possible.

## The 5 acts

### 1. Roles and permissions, and deny by default

The online store has 15 things that can be done, such as orders:read and refunds:approve. The admin role allows all 15, and the support, warehouse and finance roles allow 3 each. When support asks for refunds:approve, nothing allows it, so it is denied by default.
And an admin with an explicit deny on secrets who asks for secrets:read is denied explicitly: a deny beats any allow.

### 2. Blast radius: what a stolen credential can reach

The nightly order export runs as an admin because that was quick: a stolen token reaches all 15. The same job with only the exporter role reaches 2 of 15. An admin account with an explicit deny on secrets reaches 13.
The job needs only orders:read and reports:read, so everything else it was given is risk without benefit.

### 3. Separation of duties: two people for a refund

Alice in support creates refund R-1, and cannot approve it. Dave, an admin, creates R-2 and is refused when he tries to approve it himself: admin power does not remove the rule. Carol in finance approves R-1.
Nobody could move money alone, so one stolen account is not enough.

### 4. Access that expires by itself

Carol in finance is granted payments:write for ticks 10 to 12, for a month-end fix. At tick 9 she cannot write payments, at ticks 10 and 12 she can, and at tick 13 she cannot again. The grant is gone and nobody had to remember to remove it.

### 5. Privilege creep, and the operating system

On day 200, of 5 permissions held, 2 were not used in the last 90 days: Bob's orders:read, never used, and Carol's payments:read, last used on day 100. Those are candidates for removal. At the operating system level, an export file with permissions rw-r--r-- is readable by others;
with rw------- it is not, and its folder is rwx------. The Java SecurityManager that once limited code inside one program has been removed, so the limits that remain are outside the program: separate operating system users, containers and separate processes.

## The verdict

Start with unattended jobs and administrator access. Replace broad roles with scoped ones, measure the blast radius of each credential, make high-risk actions need two people, prefer temporary grants to permanent ones, and review usage regularly.

## How to recognise this in code you did not write

- IAM policies with `Effect: Deny` and a default deny.
- Kubernetes `Role` and `RoleBinding`, and `sudoers` entries.
- Short-lived tokens and "break glass" admin accounts.
- Approval workflows for payments and deployments.

## Where you have already met this

- Cloud IAM policies, with their explicit deny and default deny, and temporary credentials.
- Kubernetes roles and role bindings, and `sudo` rules.
- Database users that can only read, and file permissions such as `chmod 600`.
- "Four eyes" approval for payments and production deploys.
