# Detecting Attacks in Logs, Explained

## The idea in one sentence

Detection turns logs into a few alerts that people trust, so write each rule as a hypothesis about an attacker, measure it against a normal day, add context to remove the false positives, and review what it misses.

## The 5 acts

### 1. One Monday of logs

The input is one synthetic Monday at an online bank with 200 accounts: 772 events, 768 logins of which 145 failed, and 4 grants of the admin role. Each line has a time, a type, an account, an address and a place, such as "07:58:18 login u042 10.2.0.43 London success".
Hidden in it are four attacks, which only the scoring knows: A, password spraying, 80 events from 203.0.113.50 between 10:00 and 10:21; B, brute force, 31 events from 198.51.100.7 between 12:00 and 12:04; C, impossible travel, one login from Sydney at 15:25; and D, privilege change, u117 making itself an administrator at 10:40. 113 of the 772 events belong to an attack.
The rest is normal, and some of it looks like an attack: typing mistakes, two people who forgot their password (6 failures each at 09:10), five VPN users who appear in New York and then Frankfurt, a phone placed in Frankfurt, a real traveller from Paris to New York, and a break-glass admin grant at 03:00 with no ticket.

### 2. Two attacks, worked out

The first input is attack A: every failure from 203.0.113.50, 79 failures on 40 accounts between 10:00:00 and 10:21:15. Counted per account, as the brute-force rule counts, the most failures one account has in 5 minutes is 2, under the threshold of 10, so brute force stays quiet.
Counted per address, as the spraying rule counts: u101 at 10:00:00, u102 at 10:00:25, and so on to u108 at 10:02:55, which makes 8 different accounts in 20 minutes, and the spraying rule fires.
The second input is attack C: u088's two successful logins, Madrid at 15:00:00 and Sydney (192.0.2.88) at 15:25:00. The speed is worked out: 17685 km in 25 minutes is 17685 x 60 / 25 = 42444 km/h, over the 900 km/h of an airliner, so impossible travel fires.
The near miss is a real trip: u030 in Paris at 08:00 and New York at 17:00, 5837 km in 540 minutes, 5837 x 60 / 540 = 649 km/h, under 900, so no alert.

### 3. A first version of each rule

The input is five rules, each a guess about an attacker: brute-force(3), 3 failures for 1 account in 5 minutes; spraying(8), 8 accounts failing from 1 address in 20 minutes; impossible-travel, over 900 km/h and 500 km or more; privilege-change, the admin role granted by a non-approver or with no ticket; and success-after-failures, a success after 1 failure from a new address.
The result, scored against the hidden attacks: 29 alerts, 8 true and 21 false, and all four attacks A, B, C and D are noticed. Impossible travel alone raises 14 alerts, 11 of them false; success-after-failures raises 9, 7 false.
Each false alert is a look-alike: 10 from the VPN users, 5 typing mistakes, 2 + 2 forgotten passwords, 1 phone address and 1 break-glass grant. An analyst who gets 29 alerts a day, most of them false, soon stops reading them.

### 4. Tuning one threshold

The input is the normal day, without the attack events, measured the way the brute-force rule counts: the most failures one account has in 5 minutes. The worst are u010 and u011 with 6 (they forgot their password); next come four accounts with 2 (typing mistakes). The attack, B on u042 from 198.51.100.7, has 30 failures in 5 minutes, then a success at 12:04:10.
The safe range is worked out: above the normal day means more than 6, so at least 7; not above the attack means at most 30. So any threshold from 7 to 30 works; 10 is chosen, with room on both sides.
The sweep checks it: thresholds 3 and 5 give 3 alerts, 2 of them false; 10 and 25 give 1 alert, true; 40 gives no alert at all, and attack B is MISSED. A threshold is a measurement, not a guess. A slower attacker would stay under it, which is what the other rules are for.

### 5. The tuned set, and what it tells the analyst

The input is three changes: brute force needs 10 failures, impossible travel skips the bank VPN's address 198.18.0.1, and success-after-failures needs 5 failures and an address that is not one of the account's usual ones from earlier weeks. Spraying and privilege change stay as they were.
The result: 9 alerts for the day, 7 true and 2 false, and all four attacks are noticed. In time order: the 03:00 privilege change for u060 (false), u033 at 08:20 (false), spraying from 203.0.113.50 at 10:02 (A), u117 from Singapore at 10:11 (A), u117 made admin at 10:40 (D), brute force on u042 at 12:01, its travel and its success at 12:04 (B), and u088 from Sydney at 15:25 (C).
The two false ones are healthy: the break-glass grant at 03:00 should be reviewed anyway, and the phone's address placed in Frankfurt is a geolocation error. An analyst can read nine alerts.

## The verdict

Start from the attacks that matter, write one rule for each, measure every rule on real logs before enabling it, keep allow-lists and thresholds owned and reviewed, send alerts to somebody who acts on them, and test the rules with replayed attacks.

## How to recognise this in code you did not write

- Sigma rules and SIEM correlation searches.
- Cloud "impossible travel" and "unfamiliar sign-in" alerts.
- ATT&CK technique IDs T1110 and T1078 in detection documentation.
- Alert fatigue as a finding in an incident review.

## Where you have already met this

- SIEM correlation rules and "detection as code" repositories (Sigma rules).
- Cloud sign-in alerts such as "unfamiliar sign-in properties" and "atypical travel".
- Account lockout, which stops brute force but is itself a denial of service for spraying.
- MITRE ATT&CK technique pages for brute force and valid accounts.
