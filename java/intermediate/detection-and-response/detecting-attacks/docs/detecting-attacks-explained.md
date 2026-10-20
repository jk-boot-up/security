# Detecting Attacks in Logs, Explained

## The idea in one sentence

Detection turns logs into a few alerts that people trust, so write each rule as a hypothesis about an attacker, measure it against a normal day, add context to remove the false positives, and review what it misses.

## The 4 acts

### 1. One Monday of logs

The log is a normal Monday with 200 accounts, plus four attacks that the scoring knows about: password spraying from one address against forty accounts (one guess works), thirty guesses at one account in four minutes followed by a success, a valid login from Sydney twenty-five minutes after one in Paris, and the compromised account making itself an administrator.

### 2. A first version of each rule

With a brute-force threshold of three failures, an impossible-travel rule that knows nothing of the VPN, and a takeover rule that fires on a single failure before a success, the rules do see the attacks. They also produce a lot of alerts a person would have to read: the five VPN users and the phone with a wrong location,
two people who forgot their passwords, ordinary typing mistakes. The analyst who gets twenty-nine alerts, most of them false, soon stops reading them.

### 3. Tuning one threshold

The two false alarms at a threshold of 3 or 5 are the people back from holiday, who fail six times in three minutes. At 10 and 25 the rule catches the attacker's thirty guesses and nothing else. At 40 the rule is quiet and blind: the attack never reaches the threshold. The
right threshold sits between the worst normal day and the weakest attack you care about, and it is a measurement, not a guess. A real attacker may go slower than five minutes, which is what the other rules and a rate limit are for.

### 4. The tuned set, and what it tells the analyst

The tuned rules use a threshold of 10 for brute force, eight distinct accounts for spraying, the VPN address list for travel, a change ticket and a list of approvers for admin grants, and each account's usual addresses for the takeover rule. Together they raise nine alerts: seven true and two false.
The false ones are explainable and healthy: the 3 a.m. break-glass admin grant that should be reviewed anyway, and a phone whose carrier address a geolocation database places in Frankfurt. All four attacks are noticed, some by more than one rule (the spraying success is seen by the spraying rule, the impossible-travel rule and the privilege-change rule).

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
