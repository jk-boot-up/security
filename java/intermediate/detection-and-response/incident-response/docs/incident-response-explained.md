# Incident Response: Prepare, Contain, Eradicate, Recover, Learn, Explained

## The idea in one sentence

Handle an incident by preparing the six basics in advance, then containing without destroying evidence, scoping from logs, closing every way back in, rebuilding from a known-good image, watching for return, and learning from a blameless review.

## The 6 acts

### 1. The attack

The input is the attacker's script, the same in all three runs: entry into web01 through a flaw at 01:00, the database password read from the configuration at 01:05, the copying of the customer table from db01 from 01:20, and at 01:40 a cron job, an SSH key and a hidden admin account called svc-backup.
The result: at 1000 rows a minute, the 50000 customers take 50000 / 1000 = 50 minutes, so all are gone at 02:10. The attacker then has five ways back in, and each is closed by a different action: a rebuild from the image removes the cron job and the SSH key, the hidden account must be found in the logs before it can be disabled, the flaw needs a patch and the password a rotation.

### 2. What was prepared

The input: six things decided before any attack (a runbook and contact list, an alert on unusual database reads, logs kept centrally for at least 7 days, a way to snapshot memory and disk, a known-good image to rebuild from, and tested backups), for three organisations: nothing has 0 of 6, alert only has 1 of 6, and full has 6 of 6.
The result: without the alert a customer finds the breach after three days, 3 x 24 x 60 = 4320 minutes, on day 3 at 01:00; with the alert it is found after 30 minutes, at 01:30.

### 3. Response A: nothing prepared, and a panicked fix

Before the response: a customer reports the breach on day 3 at 01:00; all 50000 rows have been gone since day 0 02:10, and all five ways back in are open.
The response: reboot web01 at 01:03 (which destroys the memory evidence and leaves the cron job and SSH key in place), rotate the database password at 01:13, reconnect at 01:23, and monitor for two hours.
After the response: the copying was never stopped by the response, 4 of 5 ways back in are open, no memory, disk or logs were kept, the root cause is unknown, web01 was out of service from 01:03 to 01:23 = 20 minutes, and at 03:23 the attacker is back through the cron job on web01.

### 4. Response B: an alert, but no plan

Before the response: the alert comes at 01:30, after (01:30 - 01:20) x 1000 = 10000 rows; all five ways back in are open.
The response: isolate web01 at 01:35 (a network rule; memory kept, the route to db01 cut), rebuild it by hand with no image (120 minutes, until 03:35), reconnect at 03:45, and monitor for two hours.
After the response: the copying stopped at (01:35 - 01:20) x 1000 = 15000 rows. The rebuild removed the cron job and the SSH key, but nobody knew about svc-backup, the flaw was not patched and the password was not rotated: 3 of 5 ways open. No snapshot was taken, so no evidence; out of service 01:35 to 03:45 = 130 minutes; and at 05:45 the attacker is back through the hidden admin account svc-backup.

### 5. Response C: the playbook

Before the response: the same alert at 01:30, the same 10000 rows, five ways open.
The response contains first (isolate web01 at 01:35, end the attacker's database session at 01:37), captures memory, disk and logs with hashes (01:52), scopes the incident from the central logs (02:12: the start at 01:00 and svc-backup made at 01:40), and then closes every way back in: rotates the database password (02:22), disables svc-backup (02:25), patches the flaw (02:55) and rebuilds web01 from the known-good image (03:35). It reconnects at 03:45, and two hours of monitoring are quiet.
After the response: 15000 rows, the same as B; 0 of 5 ways open; memory, disk and logs kept; the root cause known because 30 days of central logs reach back to 01:00; out of service 01:35 to 03:45 = 130 minutes; the attacker did not come back.

### 6. The review

The input is the three runs; the result is the review table, one row per question: when it was found, how many rows were copied before the copying was stopped, how many ways back in were left open, whether evidence was kept and the root cause is known, how long web01 was out of service, whether the attacker came back, and whether the shop recovered.
A had the shortest outage (20 minutes) and still did not recover. B and C stopped the copying at the same 15000 rows and had the same 130-minute outage; only C left no way back in, kept the evidence, knows the root cause and recovered.
B and C did the same first two steps (detect, isolate web01). At step 3, B rebuilt web01 by hand (03:35) while C killed the database sessions (01:37) and went on to capture evidence and scope from logs: that is the decision a blameless review looks for.

## The verdict

Write a short runbook with names, keep logs centrally for weeks, alert on the actions that matter, practise with a tabletop exercise, isolate rather than reboot, capture evidence with hashes, rebuild from images, rotate every credential the attacker could have seen, and hold a blameless review with owners for the follow-ups.

## How to recognise this in code you did not write

- The NIST SP 800-61 Rev. 2 lifecycle diagram.
- Runbooks and on-call rotations; blameless postmortems.
- "Isolate, do not power off" in forensic guidance.
- Breach notifications that mention persistence mechanisms.

## Where you have already met this

- "Turn it off and on again", which is exactly wrong in an incident.
- Breach notifications that say the attackers were inside "for several months".
- Post-incident reviews and "blameless postmortems".
- The NIST incident response lifecycle diagram, and tabletop exercises.
