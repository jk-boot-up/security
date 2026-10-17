# Password Attacks and Defences, Explained

## The idea in one sentence

Attackers can guess passwords through the login page itself, with a dictionary against one account, a few common passwords against every account, or leaked pairs from another site, and each defence, lockout, rate limit, second factor or breach check, stops some of these and misses others.

## The 5 acts

### 1. No defences

The bank's login answers every attempt, as often as it is asked. A dictionary attack on Dana tries twenty
common passwords in order, one per tick, and finds midnight7 on the 14th guess, at tick 13. Spraying tries three common
passwords, welcome1, hunter2 and 123456, against all ten accounts. It takes four accounts, Emil, Farah, Hana and Ivan, in 23
guesses, finishing at tick 23. Each account saw at most three wrong guesses.

### 2. Lock an account after 5 wrong passwords, for 10 ticks

A lockout blocks an account for 10 ticks after 5 wrong passwords. The dictionary attack must now wait out each lock, and
it finds Dana's password at tick 33 instead of tick 13: slowed, not stopped. The spray is not affected at all. It still takes
four accounts and finishes at tick 23, because no account ever fails 5 times in a row. A lockout counts failures per account, and a
spray spreads its failures thinly across all of them.

### 3. Allow each address 5 attempts in every 10 ticks

A rate limit counts attempts per address, not per account. Each address may make 5 attempts in every 10 ticks, and the
attacker has one address. The dictionary attack now finds Dana's password at tick 23. The spray still takes four accounts,
but needs 43 ticks instead of 23. The attack runs at about half the speed. A longer wordlist would simply take longer; the
limit buys time and does not close the door.

### 4. Credential stuffing: passwords leaked from another site

Credential stuffing uses pairs of usernames and passwords leaked from another site. Ten leaked pairs are each tried once, each
from a different address. Four of them are passwords the customers reused at the bank. The lockout and the rate limit
never trigger, because every account sees one attempt and every address makes one. Four of ten accounts are taken. If Dana and Joel have
a second factor, two of the four are stopped at the second factor and two are taken. With a check against known breaches, the right passwords
are recognised as public and the login demands a new one: none are taken, and four customers are forced to choose a new password.

### 5. The bill: a lock can be used against the customer, and messages can leak

The defences have costs. First, a lockout per account can be turned against the customer: an attacker fails 5 times on Dana's
account, and then Dana, logging in from her own phone with the right password, is told the account is locked. That is a denial of
service. Locking each account-and-address pair instead lets Dana in, but an attacker with 10 addresses now gets 10 times 5, which is 50, guesses
before any is locked. Second, messages. If an unknown name says "no account with that name" and a wrong password says "wrong password", the
attacker can test which usernames exist. One message for both, "wrong username or password", hides it, at the price of a vaguer message for honest typing mistakes.

## The verdict

Layer the defences, because none of them covers every attack. Use a rate limit per address and a gentle limit per account, a second factor for
anything that matters, and a check against known breaches at sign-up and login. Give one message for every failure, never lock an account in a way an
outsider can trigger for long, and watch the logs, because the first sign of spraying or stuffing is many accounts failing once each.

## How to recognise this in code you did not write

- A login that waits longer after each wrong try.
- "We sent a code to your phone" after a correct password.
- A sign-up form that refuses a password because it appeared in a breach.
- A single message such as "wrong username or password".

## Where you have already met this

- "Your account has been locked after too many failed attempts" emails.
- A login page that asks you to wait, or to prove you are not a robot, after a few wrong tries.
- A prompt to enter a code from an app after you type your password.
- "This password has appeared in a data breach, please choose another" at sign-up.
