# The Principle of Least Privilege, Explained

## The idea in one sentence

Give each person and program only the permissions its work needs, deny everything else by default, require two people for risky actions, let temporary access expire, and remove permissions nobody uses, so that any one stolen account can reach as little as possible.

## The 5 acts

### 1. Roles and permissions, and deny by default

The online store has 15 things that can be done, written as resource:action, such as orders:read and refunds:approve. The admin role allows all 15 with the single statement allow *, and the support, warehouse and finance roles allow 3 each.
A request is checked statement by statement. When support asks for refunds:approve, none of its three statements matches, so it is denied by default.
When an admin who also has deny secrets:* asks for secrets:read, allow * matches, but deny secrets:* matches too, and a deny beats any allow: denied explicitly.

### 2. Blast radius: what a stolen credential can reach

The nightly order export needs only orders:read and reports:read. Before the fix it runs as an admin, because that was quick, and a stolen token reaches all 15 permissions: 13 of them the job never needs.
After the fix the same job has only the exporter role, and a stolen token reaches 2 of 15, both needed. Another admin account with an explicit deny on secrets reaches 13 of 15.
Whatever the job does not need is risk without benefit.

### 3. Separation of duties: two people for a refund

The rule: approving a refund needs refunds:approve, and the approver may not be the person who created it. Alice in support creates refund R-1 and cannot approve it, because support has no refunds:approve.
Dave, an admin, creates R-2 and is refused when he approves it himself: SAME_PERSON, so admin power does not remove the rule. Carol in finance approves R-1.
Nobody could move money alone, so one stolen account is not enough.

### 4. Access that expires by itself

Carol in finance gets payments:write for a month-end fix, from tick 10 until tick 13 (a tick is one step of the demo's simulated clock, and tick 13 is the first without the grant).
At tick 9 she cannot write payments, at ticks 10 and 12 she can, and at tick 13 she cannot again. The grant is gone and nobody had to remember to remove it.

### 5. Privilege creep, and the operating system

Privilege creep is permission that piles up and is never taken away. On day 200 the audit looks at 5 permissions held, and when each was last used. Unused means last used before day 200 - 90 = 110: Bob's orders:read, never used, and Carol's payments:read, last used on day 100. Those 2 are candidates for removal.
At the operating system level, the export file is first written with permissions rw-r--r--: the owner can read and write, and the group and everyone else can read. Locked to rw------- only the owner can, and its folder is rwx------.
The Java SecurityManager that once limited code inside one program is permanently disabled since JDK 24, so the limits that remain are outside the program: separate operating system users, containers and separate processes.

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
