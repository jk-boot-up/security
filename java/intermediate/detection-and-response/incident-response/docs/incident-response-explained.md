# Incident Response: Prepare, Contain, Eradicate, Recover, Learn, Explained

## The idea in one sentence

Handle an incident by preparing the six basics in advance, then containing without destroying evidence, scoping from logs, closing every way back in, rebuilding from a known-good image, watching for return, and learning from a blameless review.

## The 6 acts

### 1. The attack

The attacker exploits the application on web01, reads the database password from its configuration, connects to db01 and copies the customer table from 01:20, and at 01:40 installs a cron job and an SSH key on web01 and creates a hidden admin account called svc-backup in the shop's admin panel.

### 2. What was prepared

Three organisations face the same attack. The first has nothing in place: no runbook, no alert on database reads, no central logs, no snapshots, no images, untested backups. The second has one alert. The third has all six: a written runbook and contacts, the alert, logs kept
centrally for 30 days, a way to snapshot memory and disk, a known-good image, and tested backups.

### 3. Response A: nothing prepared, and a panicked fix

Without monitoring the breach is discovered three days later, when a customer says their data is being sold; all 50,000 rows left on the first night. The responders reboot web01 (which destroys the memory evidence and leaves the cron job and SSH key in place), rotate the database password, and reconnect. Four ways back in
are still open, and the attacker returns using the cron job. Nothing is known about what they did.

### 4. Response B: an alert, but no plan

With an alert on database reads the breach is found 30 minutes in, and isolating web01 at 01:35 stops the copying after 15,000 rows. But there are no central logs, so nobody knows about the hidden admin account; there is no image, so the rebuild takes two hours by hand and nobody is sure it is clean;
the password is not rotated and the flaw is not patched. The shop returns to service at 03:45 with three ways back in open, and the attacker returns through the hidden account.

### 5. Response C: the playbook

The alert arrives at 01:30. The playbook contains first (isolate web01 at 01:35, which keeps the memory, and end the attacker's database session), then captures memory, disk and logs with hashes, and scopes the incident from the central logs: the start at 01:00, the two hosts, and the hidden account created at 01:40.
It then closes every way back in: rotates the database password, disables svc-backup, patches the flaw, and rebuilds web01 from the known-good image (which removes the cron job and the key). At 03:45 web01 is reconnected, and two hours of monitoring are quiet.

### 6. The review

The table compares the three responses. A: found after three days, the copying never stopped by the response (all 50,000 rows had already gone), four ways back in, no evidence, no root cause, the attacker came back, not recovered. B: found in 30 minutes and 15,000 rows copied before it was stopped, but three ways in are open, no evidence, no root cause,
130 minutes out of service, the attacker came back. C: the same 15,000 rows, no ways in left, evidence kept, the root cause known, 130 minutes out of service, the attacker did not come back, recovered.
The next step of "learn" is a blameless review: which preparation would have saved the 15,000 rows? (An alert in five minutes, or a rate limit on the database account.)

## The verdict

Write a short runbook with names, keep logs centrally for weeks, alert on the actions that matter, practise with a tabletop exercise, isolate rather than reboot, capture evidence with hashes, rebuild from images, rotate every credential the attacker could have seen, and hold a blameless review with owners for the follow-ups.

## How to recognise this in code you did not write

- The NIST SP 800-61 lifecycle diagram.
- Runbooks and on-call rotations; blameless postmortems.
- "Isolate, do not power off" in forensic guidance.
- Breach notifications that mention persistence mechanisms.

## Where you have already met this

- "Turn it off and on again", which is exactly wrong in an incident.
- Breach notifications that say the attackers were inside "for several months".
- Post-incident reviews and "blameless postmortems".
- The NIST incident response lifecycle diagram, and tabletop exercises.
